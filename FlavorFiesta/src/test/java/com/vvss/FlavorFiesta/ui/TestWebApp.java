package com.vvss.FlavorFiesta.ui;

import com.vvss.FlavorFiesta.models.Comment;
import com.vvss.FlavorFiesta.models.Recipe;
import com.vvss.FlavorFiesta.models.User;
import com.vvss.FlavorFiesta.services.CommentService;
import com.vvss.FlavorFiesta.services.RecipeService;
import com.vvss.FlavorFiesta.services.UserService;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.logging.LogEntry;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import static org.junit.jupiter.api.Assertions.assertFalse;
import java.util.logging.Level;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //  meant to be used together with and @BeforeAll @AfterAll
public class TestWebApp {

    @LocalServerPort
    private int port;

    private String baseURL;
    private String loginPageURL;
    private String homePageURL;
    private String recipePageURL;
    private WebDriver driver;

    @Autowired
    UserService userService;

    @Autowired
    RecipeService recipeService;

    @Autowired
    CommentService commentService;

    User testUser;
    String testUserPassword = "test";


    @BeforeAll
    void setup() {
        baseURL = "http://localhost:" + port;
        loginPageURL = baseURL + "/login";
        homePageURL = baseURL + "/home";
        recipePageURL = baseURL + "/recipe/1";

        testUser = new User("john", "john@test.com", testUserPassword);
        userService.saveUser(testUser);

        driver = new ChromeDriver();
    }

    @AfterAll
    void tearDown() {
        userService.getAllUsers().forEach(userService::saveUser);

        // Close the browser
        driver.close();
        driver.quit();
    }

    @Test
    void testSuccessfulLogin() throws Exception {
        // Navigate to login page
        driver.get(loginPageURL);

        // Find the username and password input fields by their IDs
        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        // Enter the username and password
        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);

        // Submit the form (assuming there's a submit button)
        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        // Wait for the page to load after the submit button click
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // Wait for up to 10 seconds
        wait.until(ExpectedConditions.urlContains(homePageURL)); // Wait until the URL contains the home page url


        // Check that the sign out button exists
        WebElement signOutButton = driver.findElement(By.id("sign-out"));
        assert signOutButton != null : "Sign-out button not found";
        assert signOutButton.isDisplayed() : "Sign-out button is not displayed";
    }

    @Test
    void testFailedLogin() throws Exception {
        // Navigate to login page
        driver.get(loginPageURL);

        // Find the username and password input fields by their IDs
        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        // Enter the username and password
        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys("wrong password");

        // Submit the form (assuming there's a submit button)
        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        // Wait for the page to load after the submit button click
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10)); // Wait for up to 10 seconds
        wait.until(ExpectedConditions.urlContains(loginPageURL)); // Wait until the URL contains the home page url


        // Check that the sign-out button does not exist
        try {
            WebElement signOutButton = driver.findElement(By.id("sign-out"));
            assert false : "Sign-out button was found";
        } catch (NoSuchElementException ignored) {

        }
    }

    @Test
    void testViewAllRecipesAfterLogin() throws Exception {
        Recipe recipe1 = new Recipe(testUser, "Titlu1", "ingrediente", "instructiuni");
        Recipe recipe2 = new Recipe(testUser, "Titlu2", "ingrediente", "instructiuni");
        recipeService.saveRecipe(recipe1);
        recipeService.saveRecipe(recipe2);

        driver.get(loginPageURL);
        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));
        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);
        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(homePageURL));

        List<WebElement> recipeCards = driver.findElements(By.className("card"));
        assert !recipeCards.isEmpty() : "No recipe cards found";
        assert recipeCards.get(0).isDisplayed() : "Recipe 1 is displayed";
        assert recipeCards.get(1).isDisplayed() : "Recipe 2 is displayed";
    }

    @Test
    void testFailToViewRecipes() throws Exception {
        driver.get(loginPageURL);
        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));
        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);
        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(homePageURL));

        try {
            List<WebElement> recipeCards = driver.findElements(By.className("card"));
            assert !recipeCards.isEmpty() : "Recipe cards were found";
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void testViewRecipeComments() {
        Comment comment1 = new Comment(recipeService.getRecipeById(1L), testUser, "comentariu1");
        Comment comment2 = new Comment(recipeService.getRecipeById(1L), testUser, "comentariu2");
        commentService.saveComment(comment1);
        commentService.saveComment(comment2);

        driver.get(loginPageURL);

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);

        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(homePageURL));

        WebElement recipeCard = driver.findElement(By.className("card"));
        WebElement recipeButton = recipeCard.findElement(By.className("btn"));
        recipeButton.click();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(recipePageURL));

        WebElement commentList = driver.findElement(By.className("list-group-item"));
        assert commentList != null : "Comments were not found";
        assert commentList.isDisplayed() : "Comments are displayed";
        commentService.getAllComments().forEach(commentService::deleteComment);
    }

    @Test
    void testFailViewRecipeComments() {
        Recipe recipe1 = new Recipe(testUser, "Titlu1", "ingrediente", "instructiuni");
        recipeService.saveRecipe(recipe1);
        driver.get(loginPageURL);

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);

        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(homePageURL));

        WebElement recipeCard = driver.findElement(By.className("card"));
        WebElement recipeButton = recipeCard.findElement(By.className("btn"));
        recipeButton.click();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(recipePageURL));

        try {
            WebElement commentList = driver.findElement(By.className("list-group-item"));
            assert commentList.isDisplayed() : "Comment List was found";
        } catch (NoSuchElementException ignored) {}
    }

    @Test
    void testPostComment(){
        driver.get(loginPageURL);

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);

        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(homePageURL));

        WebElement recipeCard = driver.findElement(By.className("card"));
        WebElement recipeButton = recipeCard.findElement(By.className("btn"));
        recipeButton.click();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(recipePageURL));

        WebElement commentField = driver.findElement(By.id("comment"));
        commentField.sendKeys("Primul meu comentariu");

        WebElement submitCommButton = driver.findElement(By.id("comment-btn"));
        submitCommButton.click();

        List<WebElement> comments = driver.findElements(By.className("comment"));
        boolean commentPosted = comments.stream().anyMatch(comment -> comment.getText().contains("Primul meu comentariu"));
        assert !commentPosted: "Comment was not successfully posted";
    }

    @Test
    void testFailPostComment(){
        driver.get(loginPageURL);

        WebElement usernameField = driver.findElement(By.id("username"));
        WebElement passwordField = driver.findElement(By.id("password"));

        usernameField.sendKeys(testUser.getUsername());
        passwordField.sendKeys(testUserPassword);

        WebElement submitButton = driver.findElement(By.id("submit-button"));
        submitButton.click();

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(homePageURL));

        WebElement recipeCard = driver.findElement(By.className("card"));
        WebElement recipeButton = recipeCard.findElement(By.className("btn"));
        recipeButton.click();

        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.urlContains(recipePageURL));

        WebElement commentField = driver.findElement(By.id("comment"));
        commentField.sendKeys("Primul meu comentariu");

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("comment")));

        List<LogEntry> logEntries = driver.manage().logs().get(LogType.BROWSER).getAll();
        boolean failureLogFound = logEntries.stream()
                .anyMatch(log -> log.getLevel() == Level.SEVERE && log.getMessage().contains("Failed to post comment"));

        assertFalse(failureLogFound, "Comment should not be posted");
    }
}
