# Retry [![Build Status](https://travis-ci.org/evolution-gaming/retry.svg)](https://travis-ci.org/evolution-gaming/retry) [![Coverage Status](https://coveralls.io/repos/evolution-gaming/retry/badge.svg)](https://coveralls.io/r/evolution-gaming/retry) [![Codacy Badge](https://api.codacy.com/project/badge/Grade/a4f92715e90142fd894fbb1f6daf698d)](https://www.codacy.com/app/evolution-gaming/retry?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=evolution-gaming/retry&amp;utm_campaign=Badge_Grade) [ ![version](https://api.bintray.com/packages/evolutiongaming/maven/retry/images/download.svg) ](https://bintray.com/evolutiongaming/maven/retry/_latestVersion) [![License: MIT](https://img.shields.io/badge/License-MIT-yellowgreen.svg)](https://opensource.org/licenses/MIT)
 
```scala
import com.evolutiongaming.retry._

val request: IO[String] = ???

val strategy = Strategy.fibonacci(5.millis).cap(200.millis)
val retry = Retry[IO](strategy)
retry(request)

``` 

## Setup

```scala
resolvers += Resolver.bintrayRepo("evolutiongaming", "maven")

libraryDependencies += "com.evolutiongaming" %% "retry" % "0.0.1"
```