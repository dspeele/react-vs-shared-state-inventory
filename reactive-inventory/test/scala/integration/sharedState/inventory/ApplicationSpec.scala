package integration.sharedState.inventory

import org.scalatest.{WordSpec, BeforeAndAfterAll, Matchers}
import mongo.MongoRepoLike
import play.api.Play
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.test.FakeApplication
import scala.Some

class ApplicationSpec extends WordSpec
with Matchers
with BeforeAndAfterAll
with MongoRepoLike {

  override def beforeAll() = {
    Play.start(FakeApplication(withGlobal = Some(new controllers.Global)))
  }

  override def afterAll() = {
    Play.stop()
  }

  "Application Controller" should {
    "send 404 on a bad request" in {
      route(FakeRequest(GET, "/boom")) shouldBe None
    }

    "render the index page" in {
      val index = route(FakeRequest(GET, "/reactive-inventory")).get
      status(index) shouldBe OK
    }

    "allow retrieval of inventory" in {
      val response = route(FakeRequest(GET, "/reactive-inventory/1")).get

      status(response) shouldBe OK
      contentType(response).get == "application/json" shouldBe true
      (contentAsJson(response) \ "sku").as[String] == "1" shouldBe true
    }

    "throw 404 on retrieval of nonexistent inventory" in {
      val response = route(FakeRequest(GET, "/reactive-inventory/-1")).get

      status(response) shouldBe NOT_FOUND
      contentType(response).get == "application/json" shouldBe true
      (contentAsJson(response) \ "sku").as[String] == "-1" shouldBe true
    }

    "allow purchase of inventory" in {
      val response = route(FakeRequest(PUT, "/reactive-inventory/1/-1")).get
      status(response) shouldBe OK
      contentType(response).get == "application/json" shouldBe true
      val contentJson = contentAsJson(response)
      (contentJson \ "sku").as[String] == "1" shouldBe true
      (contentJson \ "quantity").as[Int] == -1 shouldBe true
      (contentJson \ "success").as[Boolean] shouldBe true
    }

    "disallow purchase of more inventory than is available" in {
      val getResponse = route(FakeRequest(GET, "/reactive-inventory/1")).get
      status(getResponse) shouldBe OK
      val moreThanAvailableQuantity = (contentAsJson(getResponse) \ "quantity").as[Int] + 1

      val putResponse = route(FakeRequest(PUT, s"/reactive-inventory/1/-$moreThanAvailableQuantity")).get
      status(putResponse) shouldBe OK
      contentType(putResponse).get == "application/json" shouldBe true
      val contentJson = contentAsJson(putResponse)
      (contentJson \ "sku").as[String] == "1" shouldBe true
      (contentJson \ "quantity").as[Int] == -moreThanAvailableQuantity shouldBe true
      (contentJson \ "success").as[Boolean] shouldBe false
    }

    "purchase should decrement inventory" in {
      val getResponse = route(FakeRequest(GET, "/reactive-inventory/1")).get
      status(getResponse) shouldBe OK
      val availableQuantity = (contentAsJson(getResponse) \ "quantity").as[Int]

      val putResponse = route(FakeRequest(PUT, s"/reactive-inventory/1/-1")).get
      status(putResponse) shouldBe OK
      contentType(putResponse).get == "application/json" shouldBe true
      val contentJson = contentAsJson(putResponse)
      (contentJson \ "sku").as[String] == "1" shouldBe true
      (contentJson \ "quantity").as[Int] == -1 shouldBe true
      (contentJson \ "success").as[Boolean] shouldBe true

      val getResponse2 = route(FakeRequest(GET, "/reactive-inventory/1")).get
      status(getResponse2) shouldBe OK
      (contentAsJson(getResponse2) \ "quantity").as[Int] == (availableQuantity - 1) shouldBe true
      Thread.sleep(1000) //we need this to allow database connections to finish
    }
  }
}
