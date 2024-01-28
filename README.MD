# Java snippets, mostly for blog posts

## Benchmarks
Execute `./gradlew jmh` to execute the JMH benchmarks contained in this repo.
Specific benchmarks can be selected with the `-Pincluide` option, as for example in
`./gradlew jmh -Pinclude=".*GcdBenchmark"`