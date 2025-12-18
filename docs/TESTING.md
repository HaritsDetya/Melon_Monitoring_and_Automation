# 🧪 **Testing Guide**

![Testing](https://img.shields.io/badge/Coverage-85%25-brightgreen)
![Unit Tests](https://img.shields.io/badge/Unit%20Tests-150%2B-green)
![UI Tests](https://img.shields.io/badge/UI%20Tests-50%2B-blue)

> **Panduan lengkap testing strategy dan implementasi untuk Greenhouse Monitoring App**

## 📋 **Daftar Isi**
- [Testing Strategy](#-testing-strategy)
- [Test Structure](#-test-structure)
- [Unit Testing](#-unit-testing)
- [UI Testing](#-ui-testing)
- [Integration Testing](#-integration-testing)
- [Test Data Management](#-test-data-management)
- [CI/CD Testing](#-cicd-testing)
- [Code Coverage](#-code-coverage)
- [Best Practices](#-best-practices)

## 🎯 **Testing Strategy**

### **Testing Pyramid**
```text
    ┌─────────────────┐
    │   UI Tests      │ 10-15%
    │  (Compose UI)   │
    └─────────────────┘
           │
    ┌─────────────────┐
    │ Integration     │ 20-25%
    │  Tests          │
    └─────────────────┘
           │
    ┌─────────────────┐
    │   Unit Tests    │ 60-70%
    │ (ViewModel, Use │
    │  Cases, Repo)   │
    └─────────────────┘
```

### **Test Categories**
| Category              | Tools                     | Coverage Target  | Execution Time       |
|-----------------------|---------------------------|------------------|----------------------|
| **Unit Tests**        | JUnit, MockK, Turbine     | 80%+             | Fast (< 1 min)       |
| **Integration Tests** | JUnit, Hilt Test          | 70%+             | Medium (1-5 min)     |
| **UI Tests**          | Compose Testing, Espresso | 50%+             | Slow (5-15 min)      |
| **End-to-End**        | Maestro, Detox            | 30%+             | Very Slow (> 15 min) |

## 🏗️ **Test Structure**

### **Project Structure**
```text
app/
├── src/
│ ├── main/
│ ├── test/ # Unit tests
│ │ ├── com/greenhouse/
│ │ │ ├── domain/ # Use case tests
│ │ │ ├── data/ # Repository tests
│ │ │ └── presentation/ # ViewModel tests
│ │ └── resources/ # Test resources
│ └── androidTest/ # Instrumented tests
│ ├── com/greenhouse/
│ │ ├── ui/ # Compose UI tests
│ │ └── integration/ # Integration tests
│ └── resources/ # Android test resources
└── build.gradle.kts
```

### **Dependencies**
```kotlin
// build.gradle.kts
dependencies {
    // Test dependencies
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("app.cash.turbine:turbine:1.0.0")
    testImplementation("io.mockk:mockk:1.13.8")
    
    // Android Test dependencies
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("io.mockk:mockk-android:1.13.8")
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.48")
    
    // Debug dependencies for testing
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## 🔬 **Unit Testing**

### **ViewModel Testing**
```kotlin
@HiltAndroidTest
class DashboardViewModelTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    @MockK
    lateinit var sensorRepository: SensorRepository
    
    @Inject
    @MockK  
    lateinit var controlRepository: ControlRepository
    
    private lateinit var viewModel: DashboardViewModel
    
    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)
        viewModel = DashboardViewModel(sensorRepository, controlRepository)
    }
    
    @Test
    fun `loadData should emit loading then success state`() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        val mockSensorData = SensorData.mock()
        val mockControlState = ControlState.mock()
        
        coEvery { sensorRepository.getLatestReading(greenhouseId) } returns Result.success(mockSensorData)
        coEvery { controlRepository.getControlState(greenhouseId) } returns Result.success(mockControlState)
        
        // Collect state changes
        val states = mutableListOf<DashboardState>()
        backgroundScope.launch {
            viewModel.uiState.collect { states.add(it) }
        }
        
        // When
        viewModel.loadData(greenhouseId)
        
        // Then
        Truth.assertThat(states).containsAtLeast(
            DashboardState.Loading,
            DashboardState.Success(mockSensorData, mockControlState)
        ).inOrder()
    }
    
    @Test
    fun `toggleFan should call repository and update state`() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        val initialControlState = ControlState.mock(fan = false)
        
        coEvery { controlRepository.getControlState(greenhouseId) } returns Result.success(initialControlState)
        coEvery { controlRepository.setFanState(greenhouseId, true) } returns Result.success(Unit)
        
        // When
        viewModel.toggleFan(greenhouseId, true)
        
        // Then
        coVerify { controlRepository.setFanState(greenhouseId, true) }
    }
}
```

### **Use Case Testing**
```kotlin
class GetLatestSensorReadingsUseCaseTest {
    
    private lateinit var useCase: GetLatestSensorReadingsUseCase
    private val mockRepository = mockk<SensorRepository>()
    
    @Before
    fun setup() {
        useCase = GetLatestSensorReadingsUseCase(mockRepository)
    }
    
    @Test
    fun `invoke should return sensor data from repository`() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        val expectedData = SensorData.mock()
        
        coEvery { mockRepository.getLatestReading(greenhouseId) } returns Result.success(expectedData)
        
        // When
        val result = useCase(greenhouseId)
        
        // Then
        Truth.assertThat(result.isSuccess).isTrue()
        Truth.assertThat(result.getOrNull()).isEqualTo(expectedData)
    }
    
    @Test
    fun `invoke should propagate repository errors`() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        val expectedError = Exception("Network error")
        
        coEvery { mockRepository.getLatestReading(greenhouseId) } returns Result.failure(expectedError)
        
        // When
        val result = useCase(greenhouseId)
        
        // Then
        Truth.assertThat(result.isFailure).isTrue()
        Truth.assertThat(result.exceptionOrNull()).isEqualTo(expectedError)
    }
}
```

### **Repository Testing**
```kotlin
class SensorRepositoryImplTest {
    
    private lateinit var repository: SensorRepositoryImpl
    private val mockRemoteDataSource = mockk<SensorRemoteDataSource>()
    private val mockLocalDataSource = mockk<SensorLocalDataSource>()
    private val mockMapper = SensorMapper()
    
    @Before
    fun setup() {
        repository = SensorRepositoryImpl(
            remoteDataSource = mockRemoteDataSource,
            localDataSource = mockLocalDataSource,
            mapper = mockMapper
        )
    }
    
    @Test
    fun `getLatestReading should return remote data and cache it`() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        val remoteDto = SensorDataDto.mock()
        val domainEntity = mockMapper.mapToDomain(remoteDto)
        
        coEvery { mockRemoteDataSource.getLatest(greenhouseId) } returns Result.success(remoteDto)
        coEvery { mockLocalDataSource.saveReading(any()) } returns Unit
        
        // When
        val result = repository.getLatestReading(greenhouseId)
        
        // Then
        Truth.assertThat(result.isSuccess).isTrue()
        Truth.assertThat(result.getOrNull()).isEqualTo(domainEntity)
        coVerify { mockLocalDataSource.saveReading(any()) }
    }
    
    @Test
    fun `getLatestReading should fallback to cache when remote fails`() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        val cachedDto = SensorDataDto.mock()
        val domainEntity = mockMapper.mapToDomain(cachedDto)
        
        coEvery { mockRemoteDataSource.getLatest(greenhouseId) } returns Result.failure(Exception("Network error"))
        coEvery { mockLocalDataSource.getLatest(greenhouseId) } returns cachedDto
        
        // When
        val result = repository.getLatestReading(greenhouseId)
        
        // Then
        Truth.assertThat(result.isSuccess).isTrue()
        Truth.assertThat(result.getOrNull()).isEqualTo(domainEntity)
    }
}
```

## 🎨 **UI Testing**

### **Compose UI Testing**
```kotlin
@HiltAndroidTest
class DashboardScreenTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Inject
    @MockK
    lateinit var viewModel: DashboardViewModel
    
    @Before
    fun setup() {
        hiltRule.inject()
        MockKAnnotations.init(this)
        
        // Set up mock state
        val mockState = DashboardState.Success(
            sensorData = SensorData.mock(),
            controlState = ControlState.mock()
        )
        
        every { viewModel.uiState } returns MutableStateFlow(mockState)
        every { viewModel.toggleFan(any(), any()) } returns Unit
    }
    
    @Test
    fun shouldDisplaySensorDataCards() {
        // When
        composeTestRule.setContent {
            GreenhouseAppTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Temperature").assertIsDisplayed()
        composeTestRule.onNodeWithText("Humidity").assertIsDisplayed()
        composeTestRule.onNodeWithText("pH Level").assertIsDisplayed()
        composeTestRule.onNodeWithText("TDS").assertIsDisplayed()
        composeTestRule.onNodeWithText("Water Temp").assertIsDisplayed()
    }
    
    @Test
    fun shouldDisplayControlButtons() {
        // When
        composeTestRule.setContent {
            GreenhouseAppTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
        
        // Then
        composeTestRule.onNodeWithText("Fan Control").assertIsDisplayed()
        composeTestRule.onNodeWithText("Pump Control").assertIsDisplayed()
        composeTestRule.onNodeWithText("Auto Mode").assertIsDisplayed()
    }
    
    @Test
    fun clickingFanToggleShouldCallViewModel() {
        // When
        composeTestRule.setContent {
            GreenhouseAppTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
        
        // Click fan toggle
        composeTestRule.onNodeWithTag("fan-toggle").performClick()
        
        // Then
        verify { viewModel.toggleFan(any(), true) }
    }
    
    @Test
    fun shouldDisplayLoadingState() {
        // Given
        every { viewModel.uiState } returns MutableStateFlow(DashboardState.Loading)
        
        // When
        composeTestRule.setContent {
            GreenhouseAppTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
        
        // Then
        composeTestRule.onNodeWithTag("loading-indicator").assertIsDisplayed()
    }
    
    @Test
    fun shouldDisplayErrorState() {
        // Given
        val errorMessage = "Failed to load data"
        every { viewModel.uiState } returns MutableStateFlow(DashboardState.Error(errorMessage))
        
        // When
        composeTestRule.setContent {
            GreenhouseAppTheme {
                DashboardScreen(viewModel = viewModel)
            }
        }
        
        // Then
        composeTestRule.onNodeWithText(errorMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
    }
}
```

### **Screenshot Testing**
```kotlin
class ScreenshotTests {
    
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()
    
    @Test
    fun dashboardScreenScreenshot() {
        composeTestRule.setContent {
            GreenhouseAppTheme {
                DashboardScreen(
                    viewModel = FakeDashboardViewModel(),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        // Take screenshot
        composeTestRule.onRoot()
            .captureToImage()
            .writeToFile("screenshots/dashboard.png")
    }
}

// Helper extension
fun Bitmap.writeToFile(filename: String) {
    val file = File(Environment.getExternalStorageDirectory(), filename)
    file.outputStream().use { stream ->
        compress(Bitmap.CompressFormat.PNG, 100, stream)
    }
}
```

## 🔗 **Integration Testing**

### **Database Integration Tests**
```kotlin
@RunWith(AndroidJUnit4::class)
class SensorDatabaseTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: SensorReadingDao
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context, AppDatabase::class.java
        ).build()
        dao = database.sensorReadingDao()
    }
    
    @After
    fun tearDown() {
        database.close()
    }
    
    @Test
    fun insertAndRetrieveSensorReading() = runTest {
        // Given
        val reading = SensorReadingEntity(
            id = "test-id",
            greenhouseId = "test-greenhouse",
            temperature = 28.5,
            humidity = 65.2,
            recordedAt = Instant.now()
        )
        
        // When
        dao.insert(reading)
        val retrieved = dao.getLatest("test-greenhouse")
        
        // Then
        Truth.assertThat(retrieved).isEqualTo(reading)
    }
    
    @Test
    fun deleteAllShouldRemoveAllReadings() = runTest {
        // Given
        val reading = SensorReadingEntity.mock()
        dao.insert(reading)
        
        // When
        dao.deleteAll()
        val count = dao.getCount()
        
        // Then
        Truth.assertThat(count).isEqualTo(0)
    }
}
```

### **Network Integration Tests**
```kotlin
@HiltAndroidTest
@UninstallModules(NetworkModule::class)
class SupabaseIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var supabaseClient: SupabaseClient
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
    
    @Test
    fun authenticateWithSupabase() = runTest {
        // Given
        val email = "test@example.com"
        val password = "test123"
        
        // When
        val result = runCatching {
            supabaseClient.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
        }
        
        // Then
        Truth.assertThat(result.isSuccess).isTrue()
    }
    
    @Test
    fun fetchSensorReadingsFromSupabase() = runTest {
        // Given
        val greenhouseId = "test-greenhouse-123"
        
        // When
        val result = runCatching {
            supabaseClient.from("sensor_readings")
                .select()
                .eq("greenhouse_id", greenhouseId)
                .limit(1)
                .execute()
                .decodeList<SensorReadingDto>()
        }
        
        // Then
        Truth.assertThat(result.isSuccess).isTrue()
    }
}
```

## 📊 **Test Data Management**

### **Test Data Factories**
```kotlin
object TestDataFactory {
    
    fun createSensorData(
        id: String = UUID.randomUUID().toString(),
        greenhouseId: String = "test-greenhouse-123",
        temperature: Double = 28.5,
        humidity: Double = 65.2,
        waterTemp: Double = 26.8,
        ph: Double = 6.5,
        tds: Double = 450.0,
        recordedAt: Instant = Instant.now()
    ) = SensorData(
        id = id,
        greenhouseId = greenhouseId,
        temperature = temperature,
        humidity = humidity,
        waterTemp = waterTemp,
        ph = ph,
        tds = tds,
        recordedAt = recordedAt
    )
    
    fun createControlState(
        id: String = UUID.randomUUID().toString(),
        greenhouseId: String = "test-greenhouse-123",
        fan: Boolean = false,
        pump: Boolean = false,
        autoMode: Boolean = false,
        updatedAt: Instant = Instant.now()
    ) = ControlState(
        id = id,
        greenhouseId = greenhouseId,
        fan = fan,
        pump = pump,
        autoMode = autoMode,
        updatedAt = updatedAt
    )
    
    fun createUser(
        id: String = UUID.randomUUID().toString(),
        username: String = "Test User",
        email: String = "test@example.com",
        phoneNumber: String = "+628123456789"
    ) = User(
        id = id,
        username = username,
        email = email,
        phoneNumber = phoneNumber,
        createdAt = Instant.now()
    )
}

// Extension functions for mocking
fun SensorData.mock() = TestDataFactory.createSensorData()
fun ControlState.mock() = TestDataFactory.createControlState()
fun User.mock() = TestDataFactory.createUser()
```

### **JSON Test Resources**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "greenhouse_id": "test-greenhouse-123",
  "temperature": 28.5,
  "humidity": 65.2,
  "water_temp": 26.8,
  "ph": 6.5,
  "tds": 450.0,
  "recorded_at": "2024-01-01T12:00:00Z"
}
```

```kotlin
// Load test resources
class TestResourceLoader {
    
    fun loadJson(fileName: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(fileName)
            ?: throw FileNotFoundException("File not found: $fileName")
        
        return inputStream.bufferedReader().use { it.readText() }
    }
    
    inline fun <reified T> loadJson(fileName: String): T {
        val json = loadJson(fileName)
        return Json.decodeFromString(json)
    }
}

// Usage in tests
@Test
fun testSensorReadingParsing() {
    val sensorReading: SensorReadingDto = TestResourceLoader()
        .loadJson("sensor_reading.json")
    
    Truth.assertThat(sensorReading.temperature).isEqualTo(28.5)
}
```

## 🔄 **CI/CD Testing**

### **GitHub Actions Test Workflow**
```yaml
name: Tests

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Setup JDK 17
        uses: actions/setup-java@v3

      - name: Run Unit Tests
        run: ./gradlew testDebugUnitTest

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: unit-test-results
          path: app/build/reports/tests/testDebugUnitTest/

  ui-tests:
    runs-on: macos-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Setup JDK 17
        uses: actions/setup-java@v3

      - name: Setup Android Emulator
        uses: reactivecircus/android-emulator-runner@v2
        with:
          api-level: 33
          target: google_apis
          arch: x86_64
          profile: pixel_4
          script: ./gradlew connectedDebugAndroidTest

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: ui-test-results
          path: app/build/reports/androidTests/connected/

  coverage:
    runs-on: ubuntu-latest
    needs: [unit-tests, ui-tests]

    steps:
      - name: Checkout
        uses: actions/checkout@v3

      - name: Setup JDK 17
        uses: actions/setup-java@v3

      - name: Generate Coverage Report
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage
        uses: codecov/codecov-action@v3
        with:
          file: app/build/reports/jacoco/jacocoTestReport/html/index.html
          flags: unittests
```

### **Test Coverage Requirements**
```kotlin
// jacoco.gradle.kts
jacoco {
    toolVersion = "0.8.10"
}

tasks.withType<Test> {
    configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest", "createDebugCoverageReport")
    
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
    
    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "**/com/example/databinding/*",
        "**/com/example/generated/callback/*",
        "**/android/databinding/*",
        "**/androidx/databinding/*",
        "**/di/*",
        "**/*MapperImpl*.*",
        "**/*$ViewInjector*.*",
        "**/*$ViewBinder*.*",
        "**/BuildConfig.*",
        "**/*Component*.*",
        "**/*BR*.*",
        "**/Manifest*.*",
        "**/*$Lambda$*.*",
        "**/*Companion*.*",
        "**/*Module*.*",
        "**/*Dagger*.*",
        "**/*Hilt*.*",
        "**/*MembersInjector*.*",
        "**/*_MembersInjector.class",
        "**/*_Factory*.*",
        "**/*_Provide*Factory*.*"
    )
    
    val javaClasses = fileTree("${project.buildDir}/intermediates/javac/debug") {
        exclude(fileFilter)
    }
    
    val kotlinClasses = fileTree("${project.buildDir}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }
    
    classDirectories.setFrom(files(javaClasses, kotlinClasses))
    sourceDirectories.setFrom(files("$project.projectDir/src/main/java"))
    executionData.setFrom(fileTree(project.buildDir) {
        include("jacoco/testDebugUnitTest.exec", "outputs/code_coverage/debugAndroidTest/connected/*coverage.ec")
    })
}
```

## 📈 **Code Coverage**

```bash
# Generate coverage report
./gradlew jacocoTestReport

# Verify coverage meets requirements
./gradlew jacocoCoverageVerification

# Open HTML report (Mac)
open app/build/reports/jacoco/jacocoTestReport/html/index.html

# Open HTML report (Linux)
xdg-open app/build/reports/jacoco/jacocoTestReport/html/index.html
```

## 🏆 **Best Practices**

### **Testing Guidelines**

1. Test Naming Convention
    ```kotlin
    // Use descriptive names
    @Test
    fun `login with valid credentials should succeed`()
    
    @Test
    fun `login with invalid credentials should fail`()
    
    @Test
    fun `login with empty password should show error`()
    ```

2. AAA Pattern (Arrange-Act-Assert)
    ```kotlin
    @Test
    fun testExample() {
        // Arrange
        val expected = "expected result"
        val systemUnderTest = SystemUnderTest()
        
        // Act
        val actual = systemUnderTest.doSomething()
        
        // Assert
        assertEquals(expected, actual)
    }
    ```

3. Use Coroutine Test Dispatchers
    ```kotlin
    @Test
    fun testWithCoroutines() = runTest {
        // Test coroutine code
    }
    ```

4. Mock External Dependencies
    ```kotlin
    @Test
    fun testWithMocks() {
        val mockRepository = mockk<Repository>()
        every { mockRepository.getData() } returns TestData
        
        // Test with mock
    }
    ```

### **Common Test Patterns**
- Parameterized Tests
    ```kotlin
    @RunWith(Parameterized::class)
    class LoginParameterizedTest(
        private val email: String,
        private val password: String,
        private val expectedResult: Boolean
    ) {
        
        companion object {
            @JvmStatic
            @Parameterized.Parameters
            fun data(): Collection<Array<Any>> {
                return listOf(
                    arrayOf("valid@email.com", "password123", true),
                    arrayOf("invalid", "password123", false),
                    arrayOf("valid@email.com", "", false),
                    arrayOf("", "password123", false),
                    arrayOf("", "", false)
                )
            }
        }
        
        @Test
        fun testLoginValidation() {
            val validator = LoginValidator()
            val result = validator.isValid(email, password)
            
            assertEquals(expectedResult, result)
        }
    }
    ```

- Test Fixtures
```kotlin
class SensorRepositoryTestFixture {
    
    val greenhouseId = "test-greenhouse-123"
    val mockSensorData = SensorData.mock()
    val mockRemoteDataSource = mockk<SensorRemoteDataSource>()
    val mockLocalDataSource = mockk<SensorLocalDataSource>()
    
    fun createRepository(): SensorRepositoryImpl {
        return SensorRepositoryImpl(
            remoteDataSource = mockRemoteDataSource,
            localDataSource = mockLocalDataSource,
            mapper = SensorMapper()
        )
    }
    
    fun setupRemoteSuccess() {
        coEvery { mockRemoteDataSource.getLatest(greenhouseId) } returns 
            Result.success(SensorDataDto.fromDomain(mockSensorData))
    }
    
    fun setupRemoteFailure() {
        coEvery { mockRemoteDataSource.getLatest(greenhouseId) } returns 
            Result.failure(Exception("Network error"))
    }
}

// Usage in tests
class SensorRepositoryTest {
    
    private val fixture = SensorRepositoryTestFixture()
    
    @Test
    fun testRemoteSuccess() = runTest {
        fixture.setupRemoteSuccess()
        val repository = fixture.createRepository()
        
        // Test with remote success
    }
}
```

## 🐛 **Troubleshooting**

### **Common Test Issues**
|            Issue             |                      	Solution                      |
|:----------------------------:|:---------------------------------------------------:|
| MockK not working with Hilt  |   	Use @UninstallModules to replace real modules    |
|    Coroutine test timeout    |  	Increase timeout or use runTest with dispatcher   |
|        UI tests flaky        |        	Add waitForIdle() or use awaitIdle()        |
|     Database tests slow      |          	Use in-memory database for tests          |
|    Network tests failing     |     	Mock network responses using MockWebServer     |

### **Debugging Tests**
```kotlin
// Add debug logging to tests
@Test
fun debugTest() {
    // Enable MockK verbose logging
    MockK.init(verbose = true)
    
    // Log test execution
    println("Starting test...")
    
    // Use debugger breakpoints
    // Run with --debug-jvm flag
}
```

## 📚 **References**
* [Android Testing Guide](https://developer.android.com/training/testing)
* [Compose Testing](https://developer.android.com/jetpack/compose/testing)
* [MockK Documentation](https://mockk.io/)
* [Jacoco Configuration](https://docs.gradle.org/current/userguide/jacoco_plugin.html)

___
Testing Version: 1.0.0
Last Updated: December 2025
