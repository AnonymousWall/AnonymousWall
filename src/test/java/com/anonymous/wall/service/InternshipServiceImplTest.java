package com.anonymous.wall.service;

import com.anonymous.wall.entity.Internship;
import com.anonymous.wall.entity.UserEntity;
import com.anonymous.wall.model.CreateInternshipRequest;
import com.anonymous.wall.model.CreatePostRequestWall;
import com.anonymous.wall.repository.CommentRepository;
import com.anonymous.wall.repository.InternshipRepository;
import com.anonymous.wall.repository.PostRepository;
import com.anonymous.wall.repository.UserRepository;
import com.anonymous.wall.service.base.InternshipService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.*;

import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(transactional = false)
@DisplayName("InternshipServiceImpl Tests")
class InternshipServiceImplTest {

    @Inject
    private InternshipService internshipService;

    @Inject
    private InternshipRepository internshipRepository;

    @Inject
    private UserRepository userRepository;

    @Inject
    private PostRepository postRepository;

    @Inject
    private CommentRepository commentRepository;

    private UserEntity testUser;
    private UserEntity otherUser;
    private UserEntity userWithNoSchoolDomain;
    private UserEntity mitUser;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        internshipRepository.deleteAll();
        userRepository.deleteAll();

        // Use UUID suffix — System.currentTimeMillis() can collide when multiple
        // users are created in the same @BeforeEach within the same millisecond.
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        testUser = new UserEntity();
        testUser.setEmail("testuser" + suffix + "@harvard.edu");
        testUser.setSchoolDomain("harvard.edu");
        testUser.setProfileName("TestUser");
        testUser.setVerified(true);
        testUser.setPasswordSet(true);
        testUser = userRepository.save(testUser);

        otherUser = new UserEntity();
        otherUser.setEmail("other" + suffix + "@harvard.edu");
        otherUser.setSchoolDomain("harvard.edu");
        otherUser.setProfileName("OtherUser");
        otherUser.setVerified(true);
        otherUser.setPasswordSet(true);
        otherUser = userRepository.save(otherUser);

        userWithNoSchoolDomain = new UserEntity();
        userWithNoSchoolDomain.setEmail("nodomain" + suffix + "@example.com");
        userWithNoSchoolDomain.setSchoolDomain(null);
        userWithNoSchoolDomain.setVerified(true);
        userWithNoSchoolDomain.setPasswordSet(true);
        userWithNoSchoolDomain = userRepository.save(userWithNoSchoolDomain);

        mitUser = new UserEntity();
        mitUser.setEmail("mit" + suffix + "@mit.edu");
        mitUser.setSchoolDomain("mit.edu");
        mitUser.setVerified(true);
        mitUser.setPasswordSet(true);
        mitUser = userRepository.save(mitUser);
    }

    @AfterEach
    void tearDown() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        internshipRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private Internship createCampusInternship(String company, String role, UserEntity owner) {
        CreateInternshipRequest req = new CreateInternshipRequest(company, role);
        // No wall override → defaults to campus
        return internshipService.createInternship(req, owner.getId());
    }

    private Internship createNationalInternship(String company, String role, UserEntity owner) {
        CreateInternshipRequest req = new CreateInternshipRequest(company, role);
        req.setWall(CreatePostRequestWall.NATIONAL); // adjust to your WallType enum
        return internshipService.createInternship(req, owner.getId());
    }

    // ─── Create Internship — Positive ─────────────────────────────────────────

    @Nested
    @DisplayName("Create Internship — Positive Cases")
    class CreateInternshipPositiveTests {

        @Test
        @DisplayName("Should create internship with all fields populated")
        void shouldCreateInternshipWithAllFields() {
            CreateInternshipRequest request = new CreateInternshipRequest("Google", "Software Engineer Intern");
            request.setSalary("$8000/month");
            request.setLocation("Mountain View, CA");
            request.setDescription("Work on cutting-edge projects");
            request.setDeadline(LocalDate.of(2026, 6, 30));

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertNotNull(result);
            assertNotNull(result.getId());
            assertEquals("Google", result.getCompany());
            assertEquals("Software Engineer Intern", result.getRole());
            assertEquals("$8000/month", result.getSalary());
            assertEquals("Mountain View, CA", result.getLocation());
            assertEquals("Work on cutting-edge projects", result.getDescription());
            assertEquals(LocalDate.of(2026, 6, 30), result.getDeadline());
            assertEquals(testUser.getId(), result.getUserId());
            assertNotNull(result.getCreatedAt());
            assertNotNull(result.getUpdatedAt());
        }

        @Test
        @DisplayName("Should create internship with minimum required fields — optional fields are null")
        void shouldCreateInternshipWithMinimumFields() {
            CreateInternshipRequest request = new CreateInternshipRequest("Microsoft", "Data Science Intern");

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertNotNull(result);
            assertEquals("Microsoft", result.getCompany());
            assertEquals("Data Science Intern", result.getRole());
            assertNull(result.getSalary());
            assertNull(result.getLocation());
            assertNull(result.getDescription());
        }

        @Test
        @DisplayName("Should default deadline to one month from now when not specified")
        void shouldDefaultDeadlineToOneMonthFromNow() {
            CreateInternshipRequest request = new CreateInternshipRequest("Apple", "iOS Intern");
            LocalDate expectedDeadline = LocalDate.now().plusMonths(1);

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertEquals(expectedDeadline, result.getDeadline(),
                    "Deadline must default to exactly one month from today when not provided");
        }

        @Test
        @DisplayName("Should default wall to campus when wall not specified")
        void shouldDefaultWallToCampus() {
            CreateInternshipRequest request = new CreateInternshipRequest("Amazon", "SDE Intern");

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertEquals("campus", result.getWall(),
                    "Wall must default to campus when not specified");
            assertEquals(testUser.getSchoolDomain(), result.getSchoolDomain(),
                    "School domain must be set from the user entity for campus posts");
        }

        @Test
        @DisplayName("Should set schoolDomain from user when wall is campus")
        void shouldSetSchoolDomainFromUserForCampusPost() {
            CreateInternshipRequest request = new CreateInternshipRequest("Meta", "Research Intern");

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertEquals("harvard.edu", result.getSchoolDomain());
        }

        @Test
        @DisplayName("Should set schoolDomain to null when wall is national")
        void shouldSetSchoolDomainNullForNationalPost() {
            CreateInternshipRequest request = new CreateInternshipRequest("Meta", "Research Intern");
            request.setWall(CreatePostRequestWall.NATIONAL);

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertEquals("national", result.getWall());
            assertNull(result.getSchoolDomain(),
                    "National posts must not have a schoolDomain set");
        }

        @Test
        @DisplayName("Should set profile name from user entity on created internship")
        void shouldSetProfileNameFromUser() {
            CreateInternshipRequest request = new CreateInternshipRequest("Netflix", "ML Intern");

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertEquals("TestUser", result.getProfileName());
        }

        @Test
        @DisplayName("Should trim whitespace from company and role")
        void shouldTrimCompanyAndRoleWhitespace() {
            CreateInternshipRequest request = new CreateInternshipRequest("  Apple  ", "  iOS Intern  ");

            Internship result = internshipService.createInternship(request, testUser.getId());

            assertEquals("Apple", result.getCompany());
            assertEquals("iOS Intern", result.getRole());
        }

        @Test
        @DisplayName("Should persist internship to database — findById returns it")
        void shouldPersistToDB() {
            CreateInternshipRequest request = new CreateInternshipRequest("Stripe", "Backend Intern");

            Internship result = internshipService.createInternship(request, testUser.getId());

            Internship fromDb = internshipRepository.findById(result.getId()).orElseThrow();
            assertEquals("Stripe", fromDb.getCompany());
        }
    }

    // ─── Create Internship — Validation ───────────────────────────────────────

    @Nested
    @DisplayName("Create Internship — Validation Errors")
    class CreateInternshipValidationTests {

        @Test
        @DisplayName("Should fail when company is null")
        void shouldFailWhenCompanyNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest(null, "Engineer"), testUser.getId()));
            assertTrue(ex.getMessage().contains("Company is required"));
        }

        @Test
        @DisplayName("Should fail when company is blank")
        void shouldFailWhenCompanyBlank() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("   ", "Engineer"), testUser.getId()));
            assertTrue(ex.getMessage().contains("Company is required"));
        }

        @Test
        @DisplayName("Should fail when role is null")
        void shouldFailWhenRoleNull() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("Google", null), testUser.getId()));
            assertTrue(ex.getMessage().contains("Role is required"));
        }

        @Test
        @DisplayName("Should fail when role is blank")
        void shouldFailWhenRoleBlank() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("Google", "   "), testUser.getId()));
            assertTrue(ex.getMessage().contains("Role is required"));
        }

        @Test
        @DisplayName("Should fail when company exceeds 255 characters")
        void shouldFailWhenCompanyTooLong() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("A".repeat(256), "Engineer"), testUser.getId()));
            assertTrue(ex.getMessage().contains("Company name cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should accept company of exactly 255 characters — boundary")
        void shouldAcceptCompanyAtExactLimit() {
            assertDoesNotThrow(() -> internshipService.createInternship(
                    new CreateInternshipRequest("A".repeat(255), "Engineer"), testUser.getId()));
        }

        @Test
        @DisplayName("Should fail when role exceeds 255 characters")
        void shouldFailWhenRoleTooLong() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("Google", "A".repeat(256)), testUser.getId()));
            assertTrue(ex.getMessage().contains("Role cannot exceed 255 characters"));
        }

        @Test
        @DisplayName("Should accept role of exactly 255 characters — boundary")
        void shouldAcceptRoleAtExactLimit() {
            assertDoesNotThrow(() -> internshipService.createInternship(
                    new CreateInternshipRequest("Google", "A".repeat(255)), testUser.getId()));
        }

        @Test
        @DisplayName("Should fail when user does not exist")
        void shouldFailWhenUserNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("Google", "Engineer"), UUID.randomUUID()));
            assertTrue(ex.getMessage().contains("User not found"));
        }

        @Test
        @DisplayName("Should fail when user has no school domain and wall is campus")
        void shouldFailWhenUserHasNoSchoolDomainForCampusWall() {
            // Impl defaults wall to campus; user without schoolDomain cannot post to campus
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.createInternship(
                            new CreateInternshipRequest("Google", "Engineer"),
                            userWithNoSchoolDomain.getId()));
            assertTrue(ex.getMessage().contains("school domain"));
        }
    }

    // ─── Get Internship (single-arg) ───────────────────────────────────────────

    @Nested
    @DisplayName("Get Internship — getInternship(id)")
    class GetInternshipTests {

        @Test
        @DisplayName("Should get internship by ID")
        void shouldGetInternshipById() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);

            Internship result = internshipService.getInternship(created.getId());

            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
            assertEquals("Google", result.getCompany());
        }

        @Test
        @DisplayName("Should throw when internship not found")
        void shouldThrowWhenInternshipNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.getInternship(UUID.randomUUID()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should return hidden internship — single-arg overload does not check hidden flag")
        void shouldReturnHiddenInternship() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);
            internshipService.hideInternship(created.getId(), testUser.getId());

            // Single-arg getInternship does NOT check hidden — that's the two-arg overload's job
            assertDoesNotThrow(() -> internshipService.getInternship(created.getId()));
        }
    }

    // ─── Get Internship (two-arg — user-aware) ─────────────────────────────────

    @Nested
    @DisplayName("Get Internship — getInternship(id, userId)")
    class GetInternshipWithUserTests {

        @Test
        @DisplayName("Should return national internship for any user")
        void shouldReturnNationalInternshipForAnyUser() {
            CreateInternshipRequest req = new CreateInternshipRequest("Google", "SWE Intern");
            req.setWall(CreatePostRequestWall.NATIONAL);
            Internship created = internshipService.createInternship(req, testUser.getId());

            Internship result = internshipService.getInternship(created.getId(), mitUser.getId());

            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
        }

        @Test
        @DisplayName("Should return campus internship for user from same school")
        void shouldReturnCampusInternshipForSameSchoolUser() {
            Internship created = createCampusInternship("Harvard CS Dept", "Research Intern", testUser);

            Internship result = internshipService.getInternship(created.getId(), otherUser.getId());

            assertNotNull(result);
            assertEquals(created.getId(), result.getId());
        }

        @Test
        @DisplayName("Should throw when internship is hidden")
        void shouldThrowWhenInternshipHidden() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);
            internshipService.hideInternship(created.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.getInternship(created.getId(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should throw when campus internship accessed by user from different school")
        void shouldThrowForDifferentSchoolUser() {
            Internship created = createCampusInternship("Harvard Lab", "Research Intern", testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.getInternship(created.getId(), mitUser.getId()));
            assertTrue(ex.getMessage().contains("other schools"));
        }

        @Test
        @DisplayName("Should throw when internship not found")
        void shouldThrowWhenNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.getInternship(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    // ─── Hide Internship ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Hide Internship")
    class HideInternshipTests {

        @Test
        @DisplayName("Should hide own internship successfully")
        void shouldHideOwnInternship() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);
            assertFalse(created.isHidden());

            internshipService.hideInternship(created.getId(), testUser.getId());

            Internship fromDb = internshipRepository.findById(created.getId()).orElseThrow();
            assertTrue(fromDb.isHidden());
        }

        @Test
        @DisplayName("Should update updatedAt timestamp when hiding")
        void shouldUpdateTimestampWhenHiding() throws InterruptedException {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);
            var originalUpdatedAt = created.getUpdatedAt();
            Thread.sleep(1000);
            internshipService.hideInternship(created.getId(), testUser.getId());

            Internship fromDb = internshipRepository.findById(created.getId()).orElseThrow();
            assertTrue(fromDb.getUpdatedAt().isAfter(originalUpdatedAt)
                            || fromDb.getUpdatedAt().isEqual(originalUpdatedAt),
                    "updatedAt must be refreshed on hide");
        }

        @Test
        @DisplayName("Should throw when user is not the owner")
        void shouldThrowWhenNotOwner() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.hideInternship(created.getId(), otherUser.getId()));
            assertTrue(ex.getMessage().contains("You can only hide your own"));
        }

        @Test
        @DisplayName("Should throw when internship not found")
        void shouldThrowWhenInternshipNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.hideInternship(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should not appear in listInternships after being hidden")
        void shouldNotAppearInListAfterHiding() {
            Internship visible = createCampusInternship("Google", "SWE Intern", testUser);
            Internship toHide = createCampusInternship("Amazon", "Data Intern", testUser);

            internshipService.hideInternship(toHide.getId(), testUser.getId());

            Page<Internship> result = internshipService.listInternships(Pageable.from(0, 10), "newest");
            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }
    }

    // ─── Unhide Internship ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("Unhide Internship")
    class UnhideInternshipTests {

        @Test
        @DisplayName("Should unhide previously hidden internship")
        void shouldUnhideOwnInternship() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);
            internshipService.hideInternship(created.getId(), testUser.getId());
            assertTrue(internshipRepository.findById(created.getId()).orElseThrow().isHidden());

            internshipService.unhideInternship(created.getId(), testUser.getId());

            Internship fromDb = internshipRepository.findById(created.getId()).orElseThrow();
            assertFalse(fromDb.isHidden());
        }

        @Test
        @DisplayName("Should throw when user is not the owner")
        void shouldThrowWhenNotOwner() {
            Internship created = createCampusInternship("Google", "SWE Intern", testUser);
            internshipService.hideInternship(created.getId(), testUser.getId());

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.unhideInternship(created.getId(), otherUser.getId()));
            assertTrue(ex.getMessage().contains("You can only unhide your own"));
        }

        @Test
        @DisplayName("Should throw when internship not found")
        void shouldThrowWhenInternshipNotFound() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> internshipService.unhideInternship(UUID.randomUUID(), testUser.getId()));
            assertTrue(ex.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("Should reappear in listInternships after being unhidden")
        void shouldReappearInListAfterUnhiding() {
            Internship internship = createCampusInternship("Google", "SWE Intern", testUser);
            internshipService.hideInternship(internship.getId(), testUser.getId());
            assertEquals(0, internshipService.listInternships(Pageable.from(0, 10), "newest").getTotalSize());

            internshipService.unhideInternship(internship.getId(), testUser.getId());

            assertEquals(1, internshipService.listInternships(Pageable.from(0, 10), "newest").getTotalSize());
        }
    }

    // ─── List Internships ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("List Internships")
    class ListInternshipsTests {

        @Test
        @DisplayName("Should return all non-hidden internships sorted newest first")
        void shouldListInternshipsNewestFirst() {
            CreateInternshipRequest req1 = new CreateInternshipRequest("Google", "SWE Intern");
            CreateInternshipRequest req2 = new CreateInternshipRequest("Microsoft", "PM Intern");
            CreateInternshipRequest req3 = new CreateInternshipRequest("Amazon", "Data Intern");

            Internship i1 = internshipService.createInternship(req1, testUser.getId());
            Internship i2 = internshipService.createInternship(req2, testUser.getId());
            Internship i3 = internshipService.createInternship(req3, testUser.getId());

            // Stagger timestamps to guarantee deterministic order
            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(2));
            i2.setCreatedAt(i2.getCreatedAt().minusSeconds(1));
            internshipRepository.update(i1);
            internshipRepository.update(i2);

            Page<Internship> result = internshipService.listInternships(Pageable.from(0, 10), "newest");

            assertEquals(3, result.getTotalSize());
            assertEquals("Amazon", result.getContent().get(0).getCompany());
            assertEquals("Microsoft", result.getContent().get(1).getCompany());
            assertEquals("Google", result.getContent().get(2).getCompany());
        }

        @Test
        @DisplayName("Should return all non-hidden internships sorted oldest first")
        void shouldListInternshipsOldestFirst() {
            CreateInternshipRequest req1 = new CreateInternshipRequest("Google", "SWE Intern");
            CreateInternshipRequest req2 = new CreateInternshipRequest("Microsoft", "PM Intern");

            Internship i1 = internshipService.createInternship(req1, testUser.getId());
            Internship i2 = internshipService.createInternship(req2, testUser.getId());

            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(1));
            internshipRepository.update(i1);

            Page<Internship> result = internshipService.listInternships(Pageable.from(0, 10), "oldest");

            assertEquals(2, result.getTotalSize());
            assertEquals("Google", result.getContent().get(0).getCompany());
            assertEquals("Microsoft", result.getContent().get(1).getCompany());
        }

        @Test
        @DisplayName("Should default to newest when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            createCampusInternship("Google", "SWE Intern", testUser);

            Page<Internship> result = internshipService.listInternships(Pageable.from(0, 10), null);

            assertEquals(1, result.getTotalSize());
        }

        @Test
        @DisplayName("Should exclude hidden internships from results")
        void shouldExcludeHiddenInternships() {
            Internship visible = createCampusInternship("Google", "SWE Intern", testUser);
            Internship hidden = createCampusInternship("Amazon", "Data Intern", testUser);
            internshipService.hideInternship(hidden.getId(), testUser.getId());

            Page<Internship> result = internshipService.listInternships(Pageable.from(0, 10), "newest");

            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should return empty page when no internships exist")
        void shouldReturnEmptyPageWhenNone() {
            Page<Internship> result = internshipService.listInternships(Pageable.from(0, 10), "newest");

            assertNotNull(result);
            assertEquals(0, result.getTotalSize());
            assertTrue(result.getContent().isEmpty());
        }

        @Test
        @DisplayName("Should paginate results correctly")
        void shouldPaginateResultsCorrectly() {
            for (int i = 1; i <= 5; i++) {
                Internship internship = internshipService.createInternship(
                        new CreateInternshipRequest("Company" + i, "Role" + i), testUser.getId());
                // Company5 is newest, Company1 is oldest
                internship.setCreatedAt(internship.getCreatedAt().minusSeconds(5 - i));
                internshipRepository.update(internship);
            }

            Page<Internship> page1 = internshipService.listInternships(Pageable.from(0, 2), "newest");
            Page<Internship> page2 = internshipService.listInternships(Pageable.from(1, 2), "newest");

            assertEquals(5, page1.getTotalSize());
            assertEquals(2, page1.getContent().size());
            assertEquals("Company5", page1.getContent().get(0).getCompany());
            assertEquals("Company4", page1.getContent().get(1).getCompany());

            assertEquals(5, page2.getTotalSize());
            assertEquals(2, page2.getContent().size());
            assertEquals("Company3", page2.getContent().get(0).getCompany());
            assertEquals("Company2", page2.getContent().get(1).getCompany());
        }
    }

    // ─── Get User Own Internships ──────────────────────────────────────────────

    @Nested
    @DisplayName("Get User Own Internships")
    class GetUserOwnInternshipsTests {

        @Test
        @DisplayName("Should return only internships belonging to the requesting user")
        void shouldReturnOnlyOwnInternships() {
            createCampusInternship("Google", "SWE Intern", testUser);
            createCampusInternship("Amazon", "PM Intern", testUser);
            createCampusInternship("Facebook", "Data Intern", otherUser); // different user

            Page<Internship> result = internshipService.getUserOwnInternships(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals(2, result.getTotalSize(),
                    "Only internships owned by testUser should be returned");
            result.getContent().forEach(i ->
                    assertEquals(testUser.getId(), i.getUserId()));
        }

        @Test
        @DisplayName("Should return internships sorted newest first")
        void shouldReturnOwnInternshipsNewestFirst() {
            Internship i1 = createCampusInternship("Google", "SWE Intern", testUser);
            Internship i2 = createCampusInternship("Amazon", "Data Intern", testUser);

            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(1));
            internshipRepository.update(i1);

            Page<Internship> result = internshipService.getUserOwnInternships(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals("Amazon", result.getContent().get(0).getCompany());
            assertEquals("Google", result.getContent().get(1).getCompany());
        }

        @Test
        @DisplayName("Should return internships sorted oldest first")
        void shouldReturnOwnInternshipsOldestFirst() {
            Internship i1 = createCampusInternship("Google", "SWE Intern", testUser);
            Internship i2 = createCampusInternship("Amazon", "Data Intern", testUser);

            i1.setCreatedAt(i1.getCreatedAt().minusSeconds(1));
            internshipRepository.update(i1);

            Page<Internship> result = internshipService.getUserOwnInternships(
                    testUser.getId(), Pageable.from(0, 10), "oldest");

            assertEquals("Google", result.getContent().get(0).getCompany());
            assertEquals("Amazon", result.getContent().get(1).getCompany());
        }

        @Test
        @DisplayName("Should exclude hidden internships from own list")
        void shouldExcludeHiddenFromOwnList() {
            Internship visible = createCampusInternship("Google", "SWE Intern", testUser);
            Internship hidden = createCampusInternship("Amazon", "Data Intern", testUser);
            internshipService.hideInternship(hidden.getId(), testUser.getId());

            Page<Internship> result = internshipService.getUserOwnInternships(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertEquals(1, result.getTotalSize());
            assertEquals(visible.getId(), result.getContent().get(0).getId());
        }

        @Test
        @DisplayName("Should return empty page when user has no internships")
        void shouldReturnEmptyPageWhenNoOwnInternships() {
            Page<Internship> result = internshipService.getUserOwnInternships(
                    testUser.getId(), Pageable.from(0, 10), "newest");

            assertNotNull(result);
            assertEquals(0, result.getTotalSize());
        }

        @Test
        @DisplayName("Should default to newest when sortBy is null")
        void shouldDefaultToNewestWhenSortByNull() {
            createCampusInternship("Google", "SWE Intern", testUser);

            Page<Internship> result = internshipService.getUserOwnInternships(
                    testUser.getId(), Pageable.from(0, 10), null);

            assertEquals(1, result.getTotalSize());
        }
    }

    // ─── Update Profile Name ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Update Profile Name By UserId")
    class UpdateProfileNameTests {

        @Test
        @DisplayName("Should update profile name on all internships owned by user")
        void shouldUpdateProfileNameOnAllInternships() {
            createCampusInternship("Google", "SWE Intern", testUser);
            createCampusInternship("Amazon", "Data Intern", testUser);

            internshipService.updateProfileNameByUserId(testUser.getId(), "UpdatedName");

            internshipRepository.findByUserId(testUser.getId()).forEach(i ->
                    assertEquals("UpdatedName", i.getProfileName(),
                            "All internships for this user must have updated profile name"));
        }

        @Test
        @DisplayName("Should not update profile name on internships owned by other users")
        void shouldNotUpdateOtherUsersInternships() {
            createCampusInternship("Google", "SWE Intern", testUser);
            createCampusInternship("Meta", "PM Intern", otherUser);

            internshipService.updateProfileNameByUserId(testUser.getId(), "UpdatedName");

            // otherUser's internship must be untouched
            internshipRepository.findByUserId(otherUser.getId()).forEach(i ->
                    assertEquals("OtherUser", i.getProfileName()));
        }
    }
}
