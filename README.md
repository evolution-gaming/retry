# Retry
[![Build Status](https://github.com/evolution-gaming/retry/workflows/CI/badge.svg)](https://github.com/evolution-gaming/retry/actions?query=workflow%3ACI)
[![Coverage Status](https://coveralls.io/repos/evolution-gaming/retry/badge.svg)](https://coveralls.io/r/evolution-gaming/retry)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/a4f92715e90142fd894fbb1f6daf698d)](https://www.codacy.com/app/evolution-gaming/retry?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=evolution-gaming/retry&amp;utm_campaign=Badge_Grade)
[![Version](https://img.shields.io/badge/version-click-blue)](https://evolution.jfrog.io/artifactory/api/search/latestVersion?g=com.evolutiongaming&a=retry_2.13&repos=public)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellowgreen.svg)](https://opensource.org/licenses/MIT)
 
```scala
import com.evolutiongaming.retry._
import com.evolutiongaming.retry.Retry.implicits._

val request: F[String] = ???

val strategy = Strategy
  .fibonacci(5.millis)
  .cap(200.millis)

request.retry(strategy)

``` 

## Setup

```scala
addSbtPlugin("com.evolution" % "sbt-artifactory-plugin" % "0.0.2")

libraryDependencies += "com.evolutiongaming" %% "retry" % "2.0.0"
```
