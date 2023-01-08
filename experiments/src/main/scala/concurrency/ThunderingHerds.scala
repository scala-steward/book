package concurrency

import zio._

trait FileSystem
trait FileNotFound
trait RetrievalFailure

trait FileService:
    def retrieveContents(name: String): ZIO[FileSystem, RetrievalFailure | FileNotFound, List[String]]

object FileService:
    def readFile(name: String): ZIO[FileSystem, RetrievalFailure | FileNotFound, List[String]] =
      ZIO.succeed(
        List("line1", "line2")
      ).debug("Read from FileSystem")

    val live =
      ZLayer.fromZIO(
        for
          accessCount <- Ref.make[Int](0)
          cache <- Ref.make[Map[String, List[String]]](Map.empty)
          activeRefreshes <- Ref.make[Map[String, Promise[RetrievalFailure, List[String]]]](Map.empty)
        yield Live(accessCount, cache, activeRefreshes)
      )

    case class Live(
        accessCount: Ref[Int],
        cache: Ref[Map[String, List[String]]],
        activeRefresh: Ref[Map[String, Promise[RetrievalFailure, List[String]]]]
        ) extends FileService:
            def retrieveContents(name: String): ZIO[FileSystem, RetrievalFailure | FileNotFound, List[String]] =
                for
                  currentCache <- cache.get
                  initialValue = currentCache.get(name)
                  activeValue <-
                    initialValue match
                      case Some(initValue) =>
                        ZIO.succeed(initValue)
                      case None =>
                          retrieveOrWaitForContents(name)
                yield activeValue

            def retrieveOrWaitForContents(name: String) =
              for
                currentRefreshes <- activeRefresh.get
                result <-  currentRefreshes.get(name) match
                    case Some(pendingValue) =>
                       pendingValue.await
                    case None =>
                       for
                         refreshAttempt <- Promise.make[RetrievalFailure, List[String]]
                         _ <- activeRefresh.update(x => x.+((name, refreshAttempt)))
                         contentsFromDisk <- readFile(name)
                         _ <- refreshAttempt.succeed(contentsFromDisk)
                         _ <- activeRefresh.update(x => x.-(name))
                       yield contentsFromDisk
              yield result



val users = List(
  "Bill",
  "Bruce",
  "James"
)

val herdBehavior =
  for
    fileService <- ZIO.service[FileService]
    _ <- ZIO.foreachPar(users)(user => fileService.retrieveContents("awesomeMemes"))
  yield ()

object ThunderingHerds extends ZIOAppDefault:
  def run =
    herdBehavior.provide(FileService.live, ZLayer.succeed(new FileSystem {}))