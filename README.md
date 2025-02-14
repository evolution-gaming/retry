# Retry
[![Build Status](https://github.com/evolution-gaming/retry/workflows/CI/badge.svg)](https://github.com/evolution-gaming/retry/actions?query=workflow%3ACI)
[![Coverage Status](https://coveralls.io/repos/evolution-gaming/retry/badge.svg)](https://coveralls.io/r/evolution-gaming/retry)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/61ab1bdeb772485fa4f2931338807c2a)](https://app.codacy.com/gh/evolution-gaming/retry/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Release](https://img.shields.io/github/v/release/evolution-gaming/retry)](https://evolution.jfrog.io/ui/packages/gav:%2F%2Fcom.evolutiongaming:retry_2.13)
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

libraryDependencies += "com.evolutiongaming" %% "retry" % "3.1.0"
```
