package net.mooctest;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.time.LocalDate;
import java.util.*;

public class StudentTest {

    private Student student;
    private Course course;
    private Enrollment enrollment;
    private StudentRepository studentRepository;
    private CourseRepository courseRepository;
    private EnrollmentRepository enrollmentRepository;
    private GradingPolicy gradingPolicy;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        studentRepository = new InMemoryStudentRepository();
        courseRepository = new InMemoryCourseRepository();
        enrollmentRepository = new InMemoryEnrollmentRepository();
        
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.3));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.4));
        components.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.3));
        gradingPolicy = new GradingPolicy(components);
    }

    @After
    public void tearDown() throws Exception {
    }

    // 测试Student类的构造函数和基本方法
    @Test
    public void testStudentConstructorAndGetters() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        
        assertNotNull(s.getId());
        assertEquals("John Doe", s.getName());
        assertEquals(dob, s.getDateOfBirth());
    }

    // 测试Student类名字前后有空格的情况
    @Test
    public void testStudentNameTrim() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("  John Doe  ", dob);
        assertEquals("John Doe", s.getName());
    }

    // 测试Student类设置名字
    @Test
    public void testStudentSetName() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        s.setName("Jane Smith");
        assertEquals("Jane Smith", s.getName());
    }

    // 测试Student类设置名字时trim空格
    @Test
    public void testStudentSetNameWithSpaces() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        s.setName("  Jane Smith  ");
        assertEquals("Jane Smith", s.getName());
    }

    // 测试Student类构造函数空名字异常
    @Test(expected = ValidationException.class)
    public void testStudentConstructorNullName() {
        new Student(null, LocalDate.now());
    }

    // 测试Student类构造函数空白名字异常
    @Test(expected = ValidationException.class)
    public void testStudentConstructorBlankName() {
        new Student("   ", LocalDate.now());
    }

    // 测试Student类设置空名字异常
    @Test(expected = ValidationException.class)
    public void testStudentSetNameNull() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        s.setName(null);
    }

    // 测试Student类设置空白名字异常
    @Test(expected = ValidationException.class)
    public void testStudentSetNameBlank() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        s.setName("  ");
    }

    // 测试Student类未来日期异常
    @Test(expected = ValidationException.class)
    public void testStudentFutureDateOfBirth() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        new Student("John Doe", futureDate);
    }

    // 测试Student类设置出生日期
    @Test
    public void testStudentSetDateOfBirth() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        LocalDate newDob = LocalDate.of(1999, 12, 31);
        s.setDateOfBirth(newDob);
        assertEquals(newDob, s.getDateOfBirth());
    }

    // 测试Student类设置未来出生日期异常
    @Test(expected = ValidationException.class)
    public void testStudentSetFutureDateOfBirth() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        s.setDateOfBirth(LocalDate.now().plusDays(1));
    }

    // 测试Student类设置null出生日期异常
    @Test(expected = ValidationException.class)
    public void testStudentSetNullDateOfBirth() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        s.setDateOfBirth(null);
    }

    // 测试Student类equals方法相同对象
    @Test
    public void testStudentEqualsSameObject() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        assertTrue(s.equals(s));
    }

    // 测试Student类equals方法不同类型
    @Test
    public void testStudentEqualsNotStudent() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        assertFalse(s.equals("Not a student"));
    }

    // 测试Student类equals方法不同学生
    @Test
    public void testStudentEqualsDifferentStudent() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s1 = new Student("John Doe", dob);
        Student s2 = new Student("Jane Smith", dob);
        assertFalse(s1.equals(s2));
    }

    // 测试Student类hashCode方法
    @Test
    public void testStudentHashCode() {
        LocalDate dob = LocalDate.of(2000, 1, 1);
        Student s = new Student("John Doe", dob);
        assertNotNull(s.hashCode());
    }

    // 测试Course类构造函数和基本方法
    @Test
    public void testCourseConstructorAndGetters() {
        Course c = new Course("CS101", "Introduction to Computer Science", 3);
        assertNotNull(c.getId());
        assertEquals("CS101", c.getCode());
        assertEquals("Introduction to Computer Science", c.getTitle());
        assertEquals(3, c.getCreditHours());
    }

    // 测试Course类代码转大写
    @Test
    public void testCourseCodeToUpperCase() {
        Course c = new Course("cs101", "Introduction to Computer Science", 3);
        assertEquals("CS101", c.getCode());
    }

    // 测试Course类代码trim空格
    @Test
    public void testCourseCodeTrim() {
        Course c = new Course("  cs101  ", "Introduction to Computer Science", 3);
        assertEquals("CS101", c.getCode());
    }

    // 测试Course类标题trim空格
    @Test
    public void testCourseTitleTrim() {
        Course c = new Course("CS101", "  Introduction to Computer Science  ", 3);
        assertEquals("Introduction to Computer Science", c.getTitle());
    }

    // 测试Course类设置代码
    @Test
    public void testCourseSetCode() {
        Course c = new Course("CS101", "Introduction to Computer Science", 3);
        c.setCode("CS102");
        assertEquals("CS102", c.getCode());
    }

    // 测试Course类设置代码转大写
    @Test
    public void testCourseSetCodeToUpperCase() {
        Course c = new Course("CS101", "Introduction to Computer Science", 3);
        c.setCode("cs102");
        assertEquals("CS102", c.getCode());
    }

    // 测试Course类设置标题
    @Test
    public void testCourseSetTitle() {
        Course c = new Course("CS101", "Introduction to Computer Science", 3);
        c.setTitle("Advanced Computer Science");
        assertEquals("Advanced Computer Science", c.getTitle());
    }

    // 测试Course类设置学分
    @Test
    public void testCourseSetCreditHours() {
        Course c = new Course("CS101", "Introduction to Computer Science", 3);
        c.setCreditHours(4);
        assertEquals(4, c.getCreditHours());
    }

    // 测试Course类构造函数空代码异常
    @Test(expected = ValidationException.class)
    public void testCourseConstructorNullCode() {
        new Course(null, "Title", 3);
    }

    // 测试Course类构造函数空白代码异常
    @Test(expected = ValidationException.class)
    public void testCourseConstructorBlankCode() {
        new Course("   ", "Title", 3);
    }

    // 测试Course类构造函数空标题异常
    @Test(expected = ValidationException.class)
    public void testCourseConstructorNullTitle() {
        new Course("CS101", null, 3);
    }

    // 测试Course类构造函数空白标题异常
    @Test(expected = ValidationException.class)
    public void testCourseConstructorBlankTitle() {
        new Course("CS101", "   ", 3);
    }

    // 测试Course类构造函数非正学分异常
    @Test(expected = ValidationException.class)
    public void testCourseConstructorNonPositiveCreditHours() {
        new Course("CS101", "Title", 0);
    }

    // 测试Course类构造函数负学分异常
    @Test(expected = ValidationException.class)
    public void testCourseConstructorNegativeCreditHours() {
        new Course("CS101", "Title", -1);
    }

    // 测试Course类设置空代码异常
    @Test(expected = ValidationException.class)
    public void testCourseSetCodeNull() {
        Course c = new Course("CS101", "Title", 3);
        c.setCode(null);
    }

    // 测试Course类设置空白代码异常
    @Test(expected = ValidationException.class)
    public void testCourseSetCodeBlank() {
        Course c = new Course("CS101", "Title", 3);
        c.setCode("  ");
    }

    // 测试Course类设置空标题异常
    @Test(expected = ValidationException.class)
    public void testCourseSetTitleNull() {
        Course c = new Course("CS101", "Title", 3);
        c.setTitle(null);
    }

    // 测试Course类设置空白标题异常
    @Test(expected = ValidationException.class)
    public void testCourseSetTitleBlank() {
        Course c = new Course("CS101", "Title", 3);
        c.setTitle("  ");
    }

    // 测试Course类设置非正学分异常
    @Test(expected = ValidationException.class)
    public void testCourseSetCreditHoursZero() {
        Course c = new Course("CS101", "Title", 3);
        c.setCreditHours(0);
    }

    // 测试Course类设置负学分异常
    @Test(expected = ValidationException.class)
    public void testCourseSetCreditHoursNegative() {
        Course c = new Course("CS101", "Title", 3);
        c.setCreditHours(-1);
    }

    // 测试Course类equals方法相同对象
    @Test
    public void testCourseEqualsSameObject() {
        Course c = new Course("CS101", "Title", 3);
        assertTrue(c.equals(c));
    }

    // 测试Course类equals方法不同类型
    @Test
    public void testCourseEqualsNotCourse() {
        Course c = new Course("CS101", "Title", 3);
        assertFalse(c.equals("Not a course"));
    }

    // 测试Course类equals方法不同课程
    @Test
    public void testCourseEqualsDifferentCourse() {
        Course c1 = new Course("CS101", "Title", 3);
        Course c2 = new Course("CS102", "Title", 3);
        assertFalse(c1.equals(c2));
    }

    // 测试Course类hashCode方法
    @Test
    public void testCourseHashCode() {
        Course c = new Course("CS101", "Title", 3);
        assertNotNull(c.hashCode());
    }

    // 测试Enrollment类构造函数和基本方法
    @Test
    public void testEnrollmentConstructorAndGetters() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        assertNotNull(e.getId());
        assertEquals("student123", e.getStudentId());
        assertEquals("course456", e.getCourseId());
        assertEquals(2023, e.getYear());
        assertEquals(Term.FALL, e.getTerm());
        assertEquals(EnrollmentStatus.ENROLLED, e.getStatus());
    }

    // 测试Enrollment类构造函数空学生ID异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorNullStudentId() {
        new Enrollment(null, "course456", 2023, Term.FALL);
    }

    // 测试Enrollment类构造函数空白学生ID异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorBlankStudentId() {
        new Enrollment("  ", "course456", 2023, Term.FALL);
    }

    // 测试Enrollment类构造函数空课程ID异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorNullCourseId() {
        new Enrollment("student123", null, 2023, Term.FALL);
    }

    // 测试Enrollment类构造函数空白课程ID异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorBlankCourseId() {
        new Enrollment("student123", "  ", 2023, Term.FALL);
    }

    // 测试Enrollment类构造函数非正年份异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorNonPositiveYear() {
        new Enrollment("student123", "course456", 0, Term.FALL);
    }

    // 测试Enrollment类构造函数负年份异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorNegativeYear() {
        new Enrollment("student123", "course456", -2023, Term.FALL);
    }

    // 测试Enrollment类构造函数空学期异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentConstructorNullTerm() {
        new Enrollment("student123", "course456", 2023, null);
    }

    // 测试Enrollment类drop方法
    @Test
    public void testEnrollmentDrop() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.drop();
        assertEquals(EnrollmentStatus.DROPPED, e.getStatus());
    }

    // 测试Enrollment类drop已完成注册异常
    @Test(expected = DomainException.class)
    public void testEnrollmentDropCompleted() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.complete();
        e.drop();
    }

    // 测试Enrollment类complete方法
    @Test
    public void testEnrollmentComplete() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.complete();
        assertEquals(EnrollmentStatus.COMPLETED, e.getStatus());
    }

    // 测试Enrollment类complete已退课注册异常
    @Test(expected = DomainException.class)
    public void testEnrollmentCompleteDropped() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.drop();
        e.complete();
    }

    // 测试Enrollment类markIncomplete方法
    @Test
    public void testEnrollmentMarkIncomplete() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.markIncomplete();
        assertEquals(EnrollmentStatus.INCOMPLETE, e.getStatus());
    }

    // 测试Enrollment类markIncomplete非ENROLLED状态异常
    @Test(expected = DomainException.class)
    public void testEnrollmentMarkIncompleteFromDropped() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.drop();
        e.markIncomplete();
    }

    // 测试Enrollment类markIncomplete从COMPLETED状态异常
    @Test(expected = DomainException.class)
    public void testEnrollmentMarkIncompleteFromCompleted() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.complete();
        e.markIncomplete();
    }

    // 测试Enrollment类recordGrade方法
    @Test
    public void testEnrollmentRecordGrade() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        GradeRecord record = new GradeRecord(GradeComponentType.MIDTERM, 85.0);
        e.recordGrade(record);
        assertEquals(1, e.getGradesByComponent().size());
        assertEquals(85.0, e.getGradesByComponent().get(GradeComponentType.MIDTERM).getScore(), 0.01);
    }

    // 测试Enrollment类recordGrade空记录异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentRecordGradeNull() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.recordGrade(null);
    }

    // 测试Enrollment类recordGrade已退课异常
    @Test(expected = DomainException.class)
    public void testEnrollmentRecordGradeDropped() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.drop();
        GradeRecord record = new GradeRecord(GradeComponentType.MIDTERM, 85.0);
        e.recordGrade(record);
    }

    // 测试Enrollment类getAverageScore方法
    @Test
    public void testEnrollmentGetAverageScore() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 80.0));
        e.recordGrade(new GradeRecord(GradeComponentType.FINAL, 90.0));
        e.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 85.0));
        
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.3));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.4));
        components.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.3));
        
        double avg = e.getAverageScore(components);
        assertEquals(85.5, avg, 0.01);
    }

    // 测试Enrollment类getAverageScore部分成绩
    @Test
    public void testEnrollmentGetAverageScorePartial() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 80.0));
        
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.3));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.4));
        components.put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.3));
        
        double avg = e.getAverageScore(components);
        assertEquals(24.0, avg, 0.01);
    }

    // 测试Enrollment类getAverageScore空组件异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentGetAverageScoreNullComponents() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.getAverageScore(null);
    }

    // 测试Enrollment类getAverageScore空组件集合异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentGetAverageScoreEmptyComponents() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.getAverageScore(new EnumMap<>(GradeComponentType.class));
    }

    // 测试Enrollment类equals方法相同对象
    @Test
    public void testEnrollmentEqualsSameObject() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        assertTrue(e.equals(e));
    }

    // 测试Enrollment类equals方法不同类型
    @Test
    public void testEnrollmentEqualsNotEnrollment() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        assertFalse(e.equals("Not an enrollment"));
    }

    // 测试Enrollment类equals方法不同注册
    @Test
    public void testEnrollmentEqualsDifferentEnrollment() {
        Enrollment e1 = new Enrollment("student123", "course456", 2023, Term.FALL);
        Enrollment e2 = new Enrollment("student123", "course456", 2023, Term.FALL);
        assertFalse(e1.equals(e2));
    }

    // 测试Enrollment类hashCode方法
    @Test
    public void testEnrollmentHashCode() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        assertNotNull(e.hashCode());
    }

    // 测试GradeComponent类构造函数和基本方法
    @Test
    public void testGradeComponentConstructorAndGetters() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        assertEquals(GradeComponentType.MIDTERM, gc.getType());
        assertEquals(0.3, gc.getWeight(), 0.01);
    }

    // 测试GradeComponent类构造函数空类型异常
    @Test(expected = ValidationException.class)
    public void testGradeComponentConstructorNullType() {
        new GradeComponent(null, 0.3);
    }

    // 测试GradeComponent类构造函数负权重异常
    @Test(expected = ValidationException.class)
    public void testGradeComponentConstructorNegativeWeight() {
        new GradeComponent(GradeComponentType.MIDTERM, -0.1);
    }

    // 测试GradeComponent类构造函数权重超过1异常
    @Test(expected = ValidationException.class)
    public void testGradeComponentConstructorWeightGreaterThanOne() {
        new GradeComponent(GradeComponentType.MIDTERM, 1.1);
    }

    // 测试GradeComponent类setType方法
    @Test
    public void testGradeComponentSetType() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        gc.setType(GradeComponentType.FINAL);
        assertEquals(GradeComponentType.FINAL, gc.getType());
    }

    // 测试GradeComponent类setType空类型异常
    @Test(expected = ValidationException.class)
    public void testGradeComponentSetTypeNull() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        gc.setType(null);
    }

    // 测试GradeComponent类setWeight方法
    @Test
    public void testGradeComponentSetWeight() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        gc.setWeight(0.5);
        assertEquals(0.5, gc.getWeight(), 0.01);
    }

    // 测试GradeComponent类setWeight负权重异常
    @Test(expected = ValidationException.class)
    public void testGradeComponentSetWeightNegative() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        gc.setWeight(-0.1);
    }

    // 测试GradeComponent类setWeight权重超过1异常
    @Test(expected = ValidationException.class)
    public void testGradeComponentSetWeightGreaterThanOne() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        gc.setWeight(1.1);
    }

    // 测试GradeComponent类边界值权重0
    @Test
    public void testGradeComponentWeightZero() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.0);
        assertEquals(0.0, gc.getWeight(), 0.01);
    }

    // 测试GradeComponent类边界值权重1
    @Test
    public void testGradeComponentWeightOne() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 1.0);
        assertEquals(1.0, gc.getWeight(), 0.01);
    }

    // 测试GradeComponent类equals方法相同对象
    @Test
    public void testGradeComponentEqualsSameObject() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        assertTrue(gc.equals(gc));
    }

    // 测试GradeComponent类equals方法相同类型
    @Test
    public void testGradeComponentEqualsSameType() {
        GradeComponent gc1 = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        GradeComponent gc2 = new GradeComponent(GradeComponentType.MIDTERM, 0.5);
        assertTrue(gc1.equals(gc2));
    }

    // 测试GradeComponent类equals方法不同类型
    @Test
    public void testGradeComponentEqualsNotGradeComponent() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        assertFalse(gc.equals("Not a grade component"));
    }

    // 测试GradeComponent类equals方法不同组件类型
    @Test
    public void testGradeComponentEqualsDifferentType() {
        GradeComponent gc1 = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        GradeComponent gc2 = new GradeComponent(GradeComponentType.FINAL, 0.3);
        assertFalse(gc1.equals(gc2));
    }

    // 测试GradeComponent类hashCode方法
    @Test
    public void testGradeComponentHashCode() {
        GradeComponent gc = new GradeComponent(GradeComponentType.MIDTERM, 0.3);
        assertNotNull(gc.hashCode());
    }

    // 测试GradeRecord类构造函数和基本方法
    @Test
    public void testGradeRecordConstructorAndGetters() {
        GradeRecord gr = new GradeRecord(GradeComponentType.MIDTERM, 85.0);
        assertEquals(GradeComponentType.MIDTERM, gr.getComponentType());
        assertEquals(85.0, gr.getScore(), 0.01);
    }

    // 测试GradeRecord类构造函数空类型异常
    @Test(expected = ValidationException.class)
    public void testGradeRecordConstructorNullType() {
        new GradeRecord(null, 85.0);
    }

    // 测试GradeRecord类构造函数负分数异常
    @Test(expected = ValidationException.class)
    public void testGradeRecordConstructorNegativeScore() {
        new GradeRecord(GradeComponentType.MIDTERM, -1.0);
    }

    // 测试GradeRecord类构造函数分数超过100异常
    @Test(expected = ValidationException.class)
    public void testGradeRecordConstructorScoreGreaterThan100() {
        new GradeRecord(GradeComponentType.MIDTERM, 100.1);
    }

    // 测试GradeRecord类setScore方法
    @Test
    public void testGradeRecordSetScore() {
        GradeRecord gr = new GradeRecord(GradeComponentType.MIDTERM, 85.0);
        gr.setScore(90.0);
        assertEquals(90.0, gr.getScore(), 0.01);
    }

    // 测试GradeRecord类setScore负分数异常
    @Test(expected = ValidationException.class)
    public void testGradeRecordSetScoreNegative() {
        GradeRecord gr = new GradeRecord(GradeComponentType.MIDTERM, 85.0);
        gr.setScore(-1.0);
    }

    // 测试GradeRecord类setScore分数超过100异常
    @Test(expected = ValidationException.class)
    public void testGradeRecordSetScoreGreaterThan100() {
        GradeRecord gr = new GradeRecord(GradeComponentType.MIDTERM, 85.0);
        gr.setScore(100.1);
    }

    // 测试GradeRecord类边界值分数0
    @Test
    public void testGradeRecordScoreZero() {
        GradeRecord gr = new GradeRecord(GradeComponentType.MIDTERM, 0.0);
        assertEquals(0.0, gr.getScore(), 0.01);
    }

    // 测试GradeRecord类边界值分数100
    @Test
    public void testGradeRecordScore100() {
        GradeRecord gr = new GradeRecord(GradeComponentType.MIDTERM, 100.0);
        assertEquals(100.0, gr.getScore(), 0.01);
    }

    // 测试GradingPolicy类构造函数和基本方法
    @Test
    public void testGradingPolicyConstructorAndGetters() {
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.3));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.7));
        GradingPolicy policy = new GradingPolicy(components);
        assertEquals(2, policy.getComponents().size());
    }

    // 测试GradingPolicy类构造函数空组件异常
    @Test(expected = ValidationException.class)
    public void testGradingPolicyConstructorNullComponents() {
        new GradingPolicy(null);
    }

    // 测试GradingPolicy类构造函数空组件集合异常
    @Test(expected = ValidationException.class)
    public void testGradingPolicyConstructorEmptyComponents() {
        new GradingPolicy(new EnumMap<>(GradeComponentType.class));
    }

    // 测试GradingPolicy类构造函数权重不等于1异常
    @Test(expected = ValidationException.class)
    public void testGradingPolicyConstructorWeightNotOne() {
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.3));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.6));
        new GradingPolicy(components);
    }

    // 测试GradingPolicy类getComponents返回不可修改集合
    @Test(expected = UnsupportedOperationException.class)
    public void testGradingPolicyGetComponentsUnmodifiable() {
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.3));
        components.put(GradeComponentType.FINAL, new GradeComponent(GradeComponentType.FINAL, 0.7));
        GradingPolicy policy = new GradingPolicy(components);
        policy.getComponents().put(GradeComponentType.ASSIGNMENT, new GradeComponent(GradeComponentType.ASSIGNMENT, 0.1));
    }

    // 测试ValidationUtil类requireNonBlank空值异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequireNonBlankNull() {
        ValidationUtil.requireNonBlank(null, "field");
    }

    // 测试ValidationUtil类requireNonBlank空白值异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequireNonBlankEmpty() {
        ValidationUtil.requireNonBlank("  ", "field");
    }

    // 测试ValidationUtil类requirePositive零异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequirePositiveZero() {
        ValidationUtil.requirePositive(0, "field");
    }

    // 测试ValidationUtil类requirePositive负数异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequirePositiveNegative() {
        ValidationUtil.requirePositive(-1, "field");
    }

    // 测试ValidationUtil类requireNonNegative负数异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequireNonNegativeNegative() {
        ValidationUtil.requireNonNegative(-0.1, "field");
    }

    // 测试ValidationUtil类requireBetween小于最小值异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequireBetweenLessThanMin() {
        ValidationUtil.requireBetween(0.5, 1.0, 2.0, "field");
    }

    // 测试ValidationUtil类requireBetween大于最大值异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequireBetweenGreaterThanMax() {
        ValidationUtil.requireBetween(2.5, 1.0, 2.0, "field");
    }

    // 测试ValidationUtil类requirePastOrPresent空日期异常
    @Test(expected = ValidationException.class)
    public void testValidationUtilRequirePastOrPresentNull() {
        ValidationUtil.requirePastOrPresent(null, "field");
    }

    // 测试DomainException类构造函数
    @Test
    public void testDomainExceptionConstructor() {
        DomainException ex = new DomainException("Test message");
        assertEquals("Test message", ex.getMessage());
    }

    // 测试DomainException类带原因构造函数
    @Test
    public void testDomainExceptionConstructorWithCause() {
        Exception cause = new Exception("Cause");
        DomainException ex = new DomainException("Test message", cause);
        assertEquals("Test message", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }

    // 测试ValidationException类构造函数
    @Test
    public void testValidationExceptionConstructor() {
        ValidationException ex = new ValidationException("Test message");
        assertEquals("Test message", ex.getMessage());
    }

    // 测试InMemoryStudentRepository类save方法
    @Test
    public void testStudentRepositorySave() {
        StudentRepository repo = new InMemoryStudentRepository();
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Student saved = repo.save(s);
        assertEquals(s.getId(), saved.getId());
    }

    // 测试InMemoryStudentRepository类save空学生异常
    @Test(expected = ValidationException.class)
    public void testStudentRepositorySaveNull() {
        StudentRepository repo = new InMemoryStudentRepository();
        repo.save(null);
    }

    // 测试InMemoryStudentRepository类findById方法
    @Test
    public void testStudentRepositoryFindById() {
        StudentRepository repo = new InMemoryStudentRepository();
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        repo.save(s);
        Optional<Student> found = repo.findById(s.getId());
        assertTrue(found.isPresent());
        assertEquals(s.getId(), found.get().getId());
    }

    // 测试InMemoryStudentRepository类findById不存在
    @Test
    public void testStudentRepositoryFindByIdNotFound() {
        StudentRepository repo = new InMemoryStudentRepository();
        Optional<Student> found = repo.findById("nonexistent");
        assertFalse(found.isPresent());
    }

    // 测试InMemoryStudentRepository类findByName方法
    @Test
    public void testStudentRepositoryFindByName() {
        StudentRepository repo = new InMemoryStudentRepository();
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        repo.save(s);
        Optional<Student> found = repo.findByName("John Doe");
        assertTrue(found.isPresent());
        assertEquals(s.getId(), found.get().getId());
    }

    // 测试InMemoryStudentRepository类findByName大小写不敏感
    @Test
    public void testStudentRepositoryFindByNameCaseInsensitive() {
        StudentRepository repo = new InMemoryStudentRepository();
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        repo.save(s);
        Optional<Student> found = repo.findByName("john doe");
        assertTrue(found.isPresent());
    }

    // 测试InMemoryStudentRepository类findByName空名字
    @Test
    public void testStudentRepositoryFindByNameNull() {
        StudentRepository repo = new InMemoryStudentRepository();
        Optional<Student> found = repo.findByName(null);
        assertFalse(found.isPresent());
    }

    // 测试InMemoryStudentRepository类findByName不存在
    @Test
    public void testStudentRepositoryFindByNameNotFound() {
        StudentRepository repo = new InMemoryStudentRepository();
        Optional<Student> found = repo.findByName("Nonexistent");
        assertFalse(found.isPresent());
    }

    // 测试InMemoryStudentRepository类findAll方法
    @Test
    public void testStudentRepositoryFindAll() {
        StudentRepository repo = new InMemoryStudentRepository();
        Student s1 = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Student s2 = new Student("Jane Smith", LocalDate.of(2001, 1, 1));
        repo.save(s1);
        repo.save(s2);
        List<Student> all = repo.findAll();
        assertEquals(2, all.size());
    }

    // 测试InMemoryStudentRepository类deleteById方法
    @Test
    public void testStudentRepositoryDeleteById() {
        StudentRepository repo = new InMemoryStudentRepository();
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        repo.save(s);
        repo.deleteById(s.getId());
        Optional<Student> found = repo.findById(s.getId());
        assertFalse(found.isPresent());
    }

    // 测试InMemoryCourseRepository类save方法
    @Test
    public void testCourseRepositorySave() {
        CourseRepository repo = new InMemoryCourseRepository();
        Course c = new Course("CS101", "Title", 3);
        Course saved = repo.save(c);
        assertEquals(c.getId(), saved.getId());
    }

    // 测试InMemoryCourseRepository类save空课程异常
    @Test(expected = ValidationException.class)
    public void testCourseRepositorySaveNull() {
        CourseRepository repo = new InMemoryCourseRepository();
        repo.save(null);
    }

    // 测试InMemoryCourseRepository类findById方法
    @Test
    public void testCourseRepositoryFindById() {
        CourseRepository repo = new InMemoryCourseRepository();
        Course c = new Course("CS101", "Title", 3);
        repo.save(c);
        Optional<Course> found = repo.findById(c.getId());
        assertTrue(found.isPresent());
        assertEquals(c.getId(), found.get().getId());
    }

    // 测试InMemoryCourseRepository类findById不存在
    @Test
    public void testCourseRepositoryFindByIdNotFound() {
        CourseRepository repo = new InMemoryCourseRepository();
        Optional<Course> found = repo.findById("nonexistent");
        assertFalse(found.isPresent());
    }

    // 测试InMemoryCourseRepository类findByCode方法
    @Test
    public void testCourseRepositoryFindByCode() {
        CourseRepository repo = new InMemoryCourseRepository();
        Course c = new Course("CS101", "Title", 3);
        repo.save(c);
        Optional<Course> found = repo.findByCode("CS101");
        assertTrue(found.isPresent());
        assertEquals(c.getId(), found.get().getId());
    }

    // 测试InMemoryCourseRepository类findByCode大小写不敏感
    @Test
    public void testCourseRepositoryFindByCodeCaseInsensitive() {
        CourseRepository repo = new InMemoryCourseRepository();
        Course c = new Course("CS101", "Title", 3);
        repo.save(c);
        Optional<Course> found = repo.findByCode("cs101");
        assertTrue(found.isPresent());
    }

    // 测试InMemoryCourseRepository类findByCode空代码
    @Test
    public void testCourseRepositoryFindByCodeNull() {
        CourseRepository repo = new InMemoryCourseRepository();
        Optional<Course> found = repo.findByCode(null);
        assertFalse(found.isPresent());
    }

    // 测试InMemoryCourseRepository类findByCode不存在
    @Test
    public void testCourseRepositoryFindByCodeNotFound() {
        CourseRepository repo = new InMemoryCourseRepository();
        Optional<Course> found = repo.findByCode("NONEXISTENT");
        assertFalse(found.isPresent());
    }

    // 测试InMemoryCourseRepository类findAll方法
    @Test
    public void testCourseRepositoryFindAll() {
        CourseRepository repo = new InMemoryCourseRepository();
        Course c1 = new Course("CS101", "Title1", 3);
        Course c2 = new Course("CS102", "Title2", 4);
        repo.save(c1);
        repo.save(c2);
        List<Course> all = repo.findAll();
        assertEquals(2, all.size());
    }

    // 测试InMemoryCourseRepository类deleteById方法
    @Test
    public void testCourseRepositoryDeleteById() {
        CourseRepository repo = new InMemoryCourseRepository();
        Course c = new Course("CS101", "Title", 3);
        repo.save(c);
        repo.deleteById(c.getId());
        Optional<Course> found = repo.findById(c.getId());
        assertFalse(found.isPresent());
    }

    // 测试InMemoryEnrollmentRepository类save方法
    @Test
    public void testEnrollmentRepositorySave() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        Enrollment saved = repo.save(e);
        assertEquals(e.getId(), saved.getId());
    }

    // 测试InMemoryEnrollmentRepository类save空注册异常
    @Test(expected = ValidationException.class)
    public void testEnrollmentRepositorySaveNull() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        repo.save(null);
    }

    // 测试InMemoryEnrollmentRepository类findById方法
    @Test
    public void testEnrollmentRepositoryFindById() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        repo.save(e);
        Optional<Enrollment> found = repo.findById(e.getId());
        assertTrue(found.isPresent());
        assertEquals(e.getId(), found.get().getId());
    }

    // 测试InMemoryEnrollmentRepository类findById不存在
    @Test
    public void testEnrollmentRepositoryFindByIdNotFound() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Optional<Enrollment> found = repo.findById("nonexistent");
        assertFalse(found.isPresent());
    }

    // 测试InMemoryEnrollmentRepository类findByStudentId方法
    @Test
    public void testEnrollmentRepositoryFindByStudentId() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment e1 = new Enrollment("student123", "course456", 2023, Term.FALL);
        Enrollment e2 = new Enrollment("student123", "course789", 2023, Term.SPRING);
        repo.save(e1);
        repo.save(e2);
        List<Enrollment> enrollments = repo.findByStudentId("student123");
        assertEquals(2, enrollments.size());
    }

    // 测试InMemoryEnrollmentRepository类findByCourseId方法
    @Test
    public void testEnrollmentRepositoryFindByCourseId() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment e1 = new Enrollment("student123", "course456", 2023, Term.FALL);
        Enrollment e2 = new Enrollment("student789", "course456", 2023, Term.FALL);
        repo.save(e1);
        repo.save(e2);
        List<Enrollment> enrollments = repo.findByCourseId("course456");
        assertEquals(2, enrollments.size());
    }

    // 测试InMemoryEnrollmentRepository类findAll方法
    @Test
    public void testEnrollmentRepositoryFindAll() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment e1 = new Enrollment("student123", "course456", 2023, Term.FALL);
        Enrollment e2 = new Enrollment("student789", "course789", 2023, Term.SPRING);
        repo.save(e1);
        repo.save(e2);
        List<Enrollment> all = repo.findAll();
        assertEquals(2, all.size());
    }

    // 测试InMemoryEnrollmentRepository类deleteById方法
    @Test
    public void testEnrollmentRepositoryDeleteById() {
        EnrollmentRepository repo = new InMemoryEnrollmentRepository();
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        repo.save(e);
        repo.deleteById(e.getId());
        Optional<Enrollment> found = repo.findById(e.getId());
        assertFalse(found.isPresent());
    }

    // 测试EnrollmentService类enroll方法
    @Test
    public void testEnrollmentServiceEnroll() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c = new Course("CS101", "Title", 3);
        studentRepository.save(s);
        courseRepository.save(c);
        
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        Enrollment e = service.enroll(s.getId(), c.getId(), 2023, Term.FALL);
        
        assertNotNull(e);
        assertEquals(s.getId(), e.getStudentId());
        assertEquals(c.getId(), e.getCourseId());
    }

    // 测试EnrollmentService类enroll学生不存在异常
    @Test(expected = DomainException.class)
    public void testEnrollmentServiceEnrollStudentNotFound() {
        Course c = new Course("CS101", "Title", 3);
        courseRepository.save(c);
        
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        service.enroll("nonexistent", c.getId(), 2023, Term.FALL);
    }

    // 测试EnrollmentService类enroll课程不存在异常
    @Test(expected = DomainException.class)
    public void testEnrollmentServiceEnrollCourseNotFound() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        studentRepository.save(s);
        
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        service.enroll(s.getId(), "nonexistent", 2023, Term.FALL);
    }

    // 测试EnrollmentService类enroll重复注册异常
    @Test(expected = DomainException.class)
    public void testEnrollmentServiceEnrollAlreadyEnrolled() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c = new Course("CS101", "Title", 3);
        studentRepository.save(s);
        courseRepository.save(c);
        
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        service.enroll(s.getId(), c.getId(), 2023, Term.FALL);
        service.enroll(s.getId(), c.getId(), 2023, Term.FALL);
    }

    // 测试EnrollmentService类drop方法
    @Test
    public void testEnrollmentServiceDrop() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c = new Course("CS101", "Title", 3);
        studentRepository.save(s);
        courseRepository.save(c);
        
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        Enrollment e = service.enroll(s.getId(), c.getId(), 2023, Term.FALL);
        service.drop(e.getId());
        
        Enrollment dropped = enrollmentRepository.findById(e.getId()).get();
        assertEquals(EnrollmentStatus.DROPPED, dropped.getStatus());
    }

    // 测试EnrollmentService类drop注册不存在异常
    @Test(expected = DomainException.class)
    public void testEnrollmentServiceDropNotFound() {
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        service.drop("nonexistent");
    }

    // 测试EnrollmentService类computeEnrollmentPercentage方法
    @Test
    public void testEnrollmentServiceComputeEnrollmentPercentage() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c = new Course("CS101", "Title", 3);
        studentRepository.save(s);
        courseRepository.save(c);
        
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        Enrollment e = service.enroll(s.getId(), c.getId(), 2023, Term.FALL);
        
        e.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 80.0));
        e.recordGrade(new GradeRecord(GradeComponentType.FINAL, 90.0));
        e.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 85.0));
        enrollmentRepository.save(e);
        
        double percentage = service.computeEnrollmentPercentage(e.getId());
        assertEquals(85.5, percentage, 0.01);
    }

    // 测试EnrollmentService类computeEnrollmentPercentage注册不存在异常
    @Test(expected = DomainException.class)
    public void testEnrollmentServiceComputeEnrollmentPercentageNotFound() {
        EnrollmentService service = new EnrollmentService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy);
        service.computeEnrollmentPercentage("nonexistent");
    }

    // 测试GradeService类recordGrade方法
    @Test
    public void testGradeServiceRecordGrade() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        enrollmentRepository.save(e);
        
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.recordGrade(e.getId(), GradeComponentType.MIDTERM, 85.0);
        
        Enrollment updated = enrollmentRepository.findById(e.getId()).get();
        assertEquals(85.0, updated.getGradesByComponent().get(GradeComponentType.MIDTERM).getScore(), 0.01);
    }

    // 测试GradeService类recordGrade注册不存在异常
    @Test(expected = DomainException.class)
    public void testGradeServiceRecordGradeEnrollmentNotFound() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.recordGrade("nonexistent", GradeComponentType.MIDTERM, 85.0);
    }

    // 测试GradeService类recordGrade组件不存在异常
    @Test(expected = ValidationException.class)
    public void testGradeServiceRecordGradeComponentNotInPolicy() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        enrollmentRepository.save(e);
        
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.recordGrade(e.getId(), GradeComponentType.QUIZ, 85.0);
    }

    // 测试GradeService类updateGrade方法
    @Test
    public void testGradeServiceUpdateGrade() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        enrollmentRepository.save(e);
        
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.recordGrade(e.getId(), GradeComponentType.MIDTERM, 85.0);
        service.updateGrade(e.getId(), GradeComponentType.MIDTERM, 90.0);
        
        Enrollment updated = enrollmentRepository.findById(e.getId()).get();
        assertEquals(90.0, updated.getGradesByComponent().get(GradeComponentType.MIDTERM).getScore(), 0.01);
    }

    // 测试GradeService类computePercentage方法
    @Test
    public void testGradeServiceComputePercentage() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 80.0));
        e.recordGrade(new GradeRecord(GradeComponentType.FINAL, 90.0));
        e.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 85.0));
        enrollmentRepository.save(e);
        
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        double percentage = service.computePercentage(e.getId());
        assertEquals(85.5, percentage, 0.01);
    }

    // 测试GradeService类computePercentage注册不存在异常
    @Test(expected = DomainException.class)
    public void testGradeServiceComputePercentageNotFound() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.computePercentage("nonexistent");
    }

    // 测试GradeService类computeGpa方法
    @Test
    public void testGradeServiceComputeGpa() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 93.0));
        e.recordGrade(new GradeRecord(GradeComponentType.FINAL, 93.0));
        e.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 93.0));
        enrollmentRepository.save(e);
        
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        double gpa = service.computeGpa(e.getId());
        assertEquals(4.0, gpa, 0.01);
    }

    // 测试GradeService类toGpa方法各等级
    @Test
    public void testGradeServiceToGpaA() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(4.0, service.toGpa(93.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaAMinus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(3.7, service.toGpa(90.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBPlus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(3.3, service.toGpa(87.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaB() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(3.0, service.toGpa(83.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBMinus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(2.7, service.toGpa(80.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaCPlus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(2.3, service.toGpa(77.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaC() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(2.0, service.toGpa(73.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaCMinus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(1.7, service.toGpa(70.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaDPlus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(1.3, service.toGpa(67.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaD() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(1.0, service.toGpa(63.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaDMinus() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(0.7, service.toGpa(60.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpaF() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(0.0, service.toGpa(59.0), 0.01);
    }

    // 测试GradeService类toGpa边界值
    @Test
    public void testGradeServiceToGpaBoundary92() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(3.7, service.toGpa(92.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary86() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(3.0, service.toGpa(86.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary82() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(2.7, service.toGpa(82.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary79() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(2.3, service.toGpa(79.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary76() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(2.0, service.toGpa(76.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary72() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(1.7, service.toGpa(72.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary69() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(1.3, service.toGpa(69.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary66() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(1.0, service.toGpa(66.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpaBoundary62() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(0.7, service.toGpa(62.9), 0.01);
    }

    @Test
    public void testGradeServiceToGpa0() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(0.0, service.toGpa(0.0), 0.01);
    }

    @Test
    public void testGradeServiceToGpa100() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        assertEquals(4.0, service.toGpa(100.0), 0.01);
    }

    // 测试GradeService类toGpa百分比超出范围异常
    @Test(expected = ValidationException.class)
    public void testGradeServiceToGpaNegative() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.toGpa(-1.0);
    }

    @Test(expected = ValidationException.class)
    public void testGradeServiceToGpaGreaterThan100() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.toGpa(100.1);
    }

    // 测试GradeService类ensureComponentExists方法
    @Test
    public void testGradeServiceEnsureComponentExists() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.ensureComponentExists(GradeComponentType.MIDTERM);
    }

    @Test(expected = ValidationException.class)
    public void testGradeServiceEnsureComponentExistsNotInPolicy() {
        GradeService service = new GradeService(enrollmentRepository, gradingPolicy);
        service.ensureComponentExists(GradeComponentType.QUIZ);
    }

    // 测试Transcript类构造函数
    @Test
    public void testTranscriptConstructor() {
        Transcript transcript = new Transcript();
        assertNotNull(transcript);
        assertEquals(0, transcript.getItems().size());
    }

    // 测试Transcript.LineItem类构造函数和getters
    @Test
    public void testTranscriptLineItemConstructorAndGetters() {
        Transcript.LineItem item = new Transcript.LineItem("CS101", "Title", 3, 85.0, 3.0);
        assertEquals("CS101", item.getCourseCode());
        assertEquals("Title", item.getCourseTitle());
        assertEquals(3, item.getCreditHours());
        assertEquals(85.0, item.getPercentage(), 0.01);
        assertEquals(3.0, item.getGpaPoints(), 0.01);
    }

    // 测试Transcript类addItem方法
    @Test
    public void testTranscriptAddItem() {
        Transcript transcript = new Transcript();
        Transcript.LineItem item = new Transcript.LineItem("CS101", "Title", 3, 85.0, 3.0);
        transcript.addItem(item);
        assertEquals(1, transcript.getItems().size());
    }

    // 测试Transcript类addItem空项异常
    @Test(expected = ValidationException.class)
    public void testTranscriptAddItemNull() {
        Transcript transcript = new Transcript();
        transcript.addItem(null);
    }

    // 测试Transcript类getItems返回不可修改列表
    @Test(expected = UnsupportedOperationException.class)
    public void testTranscriptGetItemsUnmodifiable() {
        Transcript transcript = new Transcript();
        transcript.getItems().add(new Transcript.LineItem("CS101", "Title", 3, 85.0, 3.0));
    }

    // 测试Transcript类computeCumulativeGpa方法
    @Test
    public void testTranscriptComputeCumulativeGpa() {
        Transcript transcript = new Transcript();
        transcript.addItem(new Transcript.LineItem("CS101", "Title1", 3, 85.0, 3.0));
        transcript.addItem(new Transcript.LineItem("CS102", "Title2", 3, 90.0, 4.0));
        double gpa = transcript.computeCumulativeGpa();
        assertEquals(3.5, gpa, 0.01);
    }

    // 测试Transcript类computeCumulativeGpa不同学分
    @Test
    public void testTranscriptComputeCumulativeGpaDifferentCredits() {
        Transcript transcript = new Transcript();
        transcript.addItem(new Transcript.LineItem("CS101", "Title1", 3, 85.0, 3.0));
        transcript.addItem(new Transcript.LineItem("CS102", "Title2", 4, 90.0, 4.0));
        double gpa = transcript.computeCumulativeGpa();
        double expected = (3.0 * 3 + 4.0 * 4) / 7.0;
        assertEquals(expected, gpa, 0.01);
    }

    // 测试Transcript类computeCumulativeGpa空列表
    @Test
    public void testTranscriptComputeCumulativeGpaEmpty() {
        Transcript transcript = new Transcript();
        double gpa = transcript.computeCumulativeGpa();
        assertEquals(0.0, gpa, 0.01);
    }

    // 测试ReportService类buildTranscript方法
    @Test
    public void testReportServiceBuildTranscript() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c1 = new Course("CS101", "Title1", 3);
        Course c2 = new Course("CS102", "Title2", 4);
        studentRepository.save(s);
        courseRepository.save(c1);
        courseRepository.save(c2);
        
        Enrollment e1 = new Enrollment(s.getId(), c1.getId(), 2023, Term.FALL);
        e1.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 80.0));
        e1.recordGrade(new GradeRecord(GradeComponentType.FINAL, 90.0));
        e1.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 85.0));
        e1.complete();
        enrollmentRepository.save(e1);
        
        Enrollment e2 = new Enrollment(s.getId(), c2.getId(), 2023, Term.SPRING);
        e2.recordGrade(new GradeRecord(GradeComponentType.MIDTERM, 93.0));
        e2.recordGrade(new GradeRecord(GradeComponentType.FINAL, 93.0));
        e2.recordGrade(new GradeRecord(GradeComponentType.ASSIGNMENT, 93.0));
        e2.markIncomplete();
        enrollmentRepository.save(e2);
        
        GradeService gradeService = new GradeService(enrollmentRepository, gradingPolicy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy, gradeService);
        
        Transcript transcript = reportService.buildTranscript(s.getId());
        assertEquals(2, transcript.getItems().size());
    }

    // 测试ReportService类buildTranscript学生不存在异常
    @Test(expected = DomainException.class)
    public void testReportServiceBuildTranscriptStudentNotFound() {
        GradeService gradeService = new GradeService(enrollmentRepository, gradingPolicy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy, gradeService);
        reportService.buildTranscript("nonexistent");
    }

    // 测试ReportService类buildTranscript课程不存在异常
    @Test(expected = DomainException.class)
    public void testReportServiceBuildTranscriptCourseNotFound() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        studentRepository.save(s);
        
        Enrollment e = new Enrollment(s.getId(), "nonexistent", 2023, Term.FALL);
        e.complete();
        enrollmentRepository.save(e);
        
        GradeService gradeService = new GradeService(enrollmentRepository, gradingPolicy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy, gradeService);
        reportService.buildTranscript(s.getId());
    }

    // 测试ReportService类buildTranscript跳过ENROLLED状态
    @Test
    public void testReportServiceBuildTranscriptSkipEnrolled() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c = new Course("CS101", "Title", 3);
        studentRepository.save(s);
        courseRepository.save(c);
        
        Enrollment e = new Enrollment(s.getId(), c.getId(), 2023, Term.FALL);
        enrollmentRepository.save(e);
        
        GradeService gradeService = new GradeService(enrollmentRepository, gradingPolicy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy, gradeService);
        
        Transcript transcript = reportService.buildTranscript(s.getId());
        assertEquals(0, transcript.getItems().size());
    }

    // 测试ReportService类buildTranscript跳过DROPPED状态
    @Test
    public void testReportServiceBuildTranscriptSkipDropped() {
        Student s = new Student("John Doe", LocalDate.of(2000, 1, 1));
        Course c = new Course("CS101", "Title", 3);
        studentRepository.save(s);
        courseRepository.save(c);
        
        Enrollment e = new Enrollment(s.getId(), c.getId(), 2023, Term.FALL);
        e.drop();
        enrollmentRepository.save(e);
        
        GradeService gradeService = new GradeService(enrollmentRepository, gradingPolicy);
        ReportService reportService = new ReportService(studentRepository, courseRepository, enrollmentRepository, gradingPolicy, gradeService);
        
        Transcript transcript = reportService.buildTranscript(s.getId());
        assertEquals(0, transcript.getItems().size());
    }

    // 测试Term枚举所有值
    @Test
    public void testTermEnumValues() {
        assertEquals(4, Term.values().length);
        assertEquals(Term.SPRING, Term.valueOf("SPRING"));
        assertEquals(Term.SUMMER, Term.valueOf("SUMMER"));
        assertEquals(Term.FALL, Term.valueOf("FALL"));
        assertEquals(Term.WINTER, Term.valueOf("WINTER"));
    }

    // 测试EnrollmentStatus枚举所有值
    @Test
    public void testEnrollmentStatusEnumValues() {
        assertEquals(4, EnrollmentStatus.values().length);
        assertEquals(EnrollmentStatus.ENROLLED, EnrollmentStatus.valueOf("ENROLLED"));
        assertEquals(EnrollmentStatus.DROPPED, EnrollmentStatus.valueOf("DROPPED"));
        assertEquals(EnrollmentStatus.COMPLETED, EnrollmentStatus.valueOf("COMPLETED"));
        assertEquals(EnrollmentStatus.INCOMPLETE, EnrollmentStatus.valueOf("INCOMPLETE"));
    }

    // 测试GradeComponentType枚举所有值
    @Test
    public void testGradeComponentTypeEnumValues() {
        assertEquals(7, GradeComponentType.values().length);
        assertEquals(GradeComponentType.ASSIGNMENT, GradeComponentType.valueOf("ASSIGNMENT"));
        assertEquals(GradeComponentType.QUIZ, GradeComponentType.valueOf("QUIZ"));
        assertEquals(GradeComponentType.MIDTERM, GradeComponentType.valueOf("MIDTERM"));
        assertEquals(GradeComponentType.FINAL, GradeComponentType.valueOf("FINAL"));
        assertEquals(GradeComponentType.PROJECT, GradeComponentType.valueOf("PROJECT"));
        assertEquals(GradeComponentType.PARTICIPATION, GradeComponentType.valueOf("PARTICIPATION"));
        assertEquals(GradeComponentType.EXTRA_CREDIT, GradeComponentType.valueOf("EXTRA_CREDIT"));
    }

    // 测试Enrollment类getGradesByComponent返回不可修改映射
    @Test(expected = UnsupportedOperationException.class)
    public void testEnrollmentGetGradesByComponentUnmodifiable() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        e.getGradesByComponent().put(GradeComponentType.MIDTERM, new GradeRecord(GradeComponentType.MIDTERM, 85.0));
    }

    // 测试Student在当天出生日期
    @Test
    public void testStudentDateOfBirthToday() {
        LocalDate today = LocalDate.now();
        Student s = new Student("John Doe", today);
        assertEquals(today, s.getDateOfBirth());
    }

    // 测试Enrollment类getAverageScore当总权重为零的情况
    @Test(expected = ValidationException.class)
    public void testEnrollmentGetAverageScoreZeroTotalWeight() {
        Enrollment e = new Enrollment("student123", "course456", 2023, Term.FALL);
        Map<GradeComponentType, GradeComponent> components = new EnumMap<>(GradeComponentType.class);
        components.put(GradeComponentType.MIDTERM, new GradeComponent(GradeComponentType.MIDTERM, 0.0));
        e.getAverageScore(components);
    }

    // 测试ValidationUtil类requireNonNegative边界值0
    @Test
    public void testValidationUtilRequireNonNegativeZero() {
        ValidationUtil.requireNonNegative(0.0, "field");
    }

    // 测试ValidationUtil类requireNonNegative正值
    @Test
    public void testValidationUtilRequireNonNegativePositive() {
        ValidationUtil.requireNonNegative(1.0, "field");
    }
}
