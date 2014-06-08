/*import org.specs2.mutable._
import play.api.test._
import play.api.test.Helpers._
import org.specs2.main.Execute

class LoginLogoutSpec extends Specification {

  "Tests for Customer Log in and Log out" should {

   

    "Test from within a browser" in {
      running(FakeApplication()) {
      running(TestServer(7055), FIREFOX) { browser =>
        //first register a customer to make sure user is exist
        browser.goTo("http://localhost:7055/register")
        browser.$("#email").text("loginTest@inbay.co.uk")
        browser.$("#password").text("secretOne")
        browser.$("#register").submit()
       
        //check with wrong email and correct pass
        browser.goTo("http://localhost:7055/login")
        browser.$("#email").text("loginTestWrong@inbay.co.uk")
        browser.$("#password").text("secretOne")
        browser.$("#login").submit()
        browser.pageSource must contain("Invalid email or password")
        
        //check with correct email and wrong pass
        browser.goTo("http://localhost:7055/login")
        browser.$("#email").text("loginTest@inbay.co.uk")
        browser.$("#password").text("secretOneWrong")
        browser.$("#login").submit()
        browser.pageSource must contain("Invalid email or password")
        
        //login to the system with created user credentials 
        browser.goTo("http://localhost:7055/login")
        browser.$("#email").text("loginTest@inbay.co.uk")
        browser.$("#password").text("secretOne")
        browser.$("#login").submit()
        //page must display user name
        browser.pageSource must contain("loginTest@inbay.co.uk")
        //if user is logged in system should show the testlogin page with user email
         browser.goTo("http://localhost:7055/testlogin")
         browser.pageSource must contain("loginTest@inbay.co.uk")
        
        //log out from the sys
         browser.goTo("http://localhost:7055/logout")
         //msg must be displayed
         browser.pageSource must contain("You've been logged out")
        //try to access testlogin page
         browser.goTo("http://localhost:7055/testlogin")
         //loin page should be prompted if the user has successfully logged out
         browser.url must contain("login")
        
        
     
      }
    }
    }

  }
}*/