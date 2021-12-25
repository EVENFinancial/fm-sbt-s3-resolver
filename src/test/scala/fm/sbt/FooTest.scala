//package fm.sbt
//
////import cats.effect.IO
////import cats.effect.testing.scalatest.{ AssertingSyntax, AsyncIOSpec }
//import com.dimafeng.testcontainers.{Container, ForAllTestContainer, LocalStackV2Container}
////import org.testcontainers.containers.localstack.LocalStackContainer
//import org.testcontainers.containers.localstack.LocalStackContainer.Service
////import dev.faustin0.importer.domain.DatasetFileLocation
////import dev.faustin0.importer.infrastructure.S3BucketLoader
//import org.scalatest.funspec.AsyncFunSpec
////import software.amazon.awssdk.services.s3.model.{ CreateBucketRequest, PutObjectRequest }
//
//import java.nio.file.Paths
//import scala.io.Source
//import scala.xml.{ Elem, XML }
//
//class S3BucketReaderIT extends AsyncFunSpec with ForAllTestContainer {
//  override val container: LocalStackV2Container = LocalStackV2Container(services = List(Service.S3))
//
//  override def afterStart(): Unit =
//    Containers
//      .createS3Client(localStack)
//      .use { s3Client =>
//        for {
//          fileSource     <- IO(getClass.getClassLoader.getResource("bus-stop-dataset.xml"))
//          createBucketReq = CreateBucketRequest.builder().bucket("bus-stops").build()
//          putObjReq       = PutObjectRequest.builder().bucket("bus-stops").key("bus-stop-dataset.xml").build()
//          _              <- IO(s3Client.createBucket(createBucketReq))
//          _              <- IO(s3Client.putObject(putObjReq, Paths.get(fileSource.toURI)))
//        } yield ()
//      }
//      .unsafeRunSync()
//
//  "should load a file from a bucket" in {
//
//    val expected = XML.loadString(Source.fromResource("bus-stop-dataset.xml").mkString)
//
////    Containers
////      .createS3Client(localStack)
////      .map(new S3BucketLoader(_))
////      .use { sut =>
////        sut.load(DatasetFileLocation("bus-stops", "bus-stop-dataset.xml"))
////      }
////      .asserting { dataset =>
////        assert(xmlEqualsIgnoringWhiteSpaces(actual = dataset.content, expected))
////      }
//
//  }
//
//}