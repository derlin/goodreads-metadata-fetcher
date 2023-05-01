# Module GoodReads Metadata Fetcher

> **Warning**
>
> This repository is currently **on hold**, due to too many changes and restrictions with the new GoodReads interface and the lack of an official API.

| [![Code](https://img.shields.io/badge/code-github.com-informational.svg)](https://github.com/derlin/goodreads-metadata-fetcher) | [![Documentation](https://img.shields.io/badge/documentation-derlin.io-informational.svg)](https://derlin.github.io/goodreads-metadata-fetcher/) | ![main workflow](https://github.com/derlin/goodreads-metadata-fetcher/actions/workflows/main.yaml/badge.svg) 
| :-----------: | :---------: | :---------:

This Kotlin Library implements a basic metadata lookup for [GoodReads](https://www.goodreads.com/).
It is compatible with Android.

## Installation

This library is published on GitHub Packages. To learn how to use GitHub Packages, see:
* for Gradle projects: [Working with the Gradle registry](
   https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry)
* for Maven projects: [Working with the Apache Maven registry](
  https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)

You can also build the library (keep reading) and then run `./gradlew publishToMavenLocal` to make it available on your machine.

### Build the library

After cloning the repo, run the following to install the package on your local maven repo:

```shell
./gradlew publishToMavenLocal
```

### Import the library

Given projects on the same computer.

**Gradle Project** open you module `build.gradle` and add:

```groovy
repositories {
    // ...
    mavenLocal() // <- use maven locally
}

dependencies {
    // ...
    implementation 'ch.derlin:goodreads-metadata-fetcher:1.0.0-SNAPSHOT'
}
```

**Maven** open your `pom.xml` and add:

```xml
<dependency>
    <groupId>ch.derlin</groupId>
    <artifactId>goodreads-metadata-fetcher</artifactId>
    <version>1.0.0-SNAPHOST</version>
</dependency>
```

## Usage

*NOTE*: this library might sometimes seem slow, but this is mostly due to GoodReads itself being slow ;).

### Search GoodReads and get Books metadata (interactively)

You will usually first do a search on GoodReads, then select a book in the list based oh on some criteria (or user input), 
and finally get the selected book's metadata.

This library allows you to do so easily, using the `GoodReadsLookup` class:

1. create a `GoodReadsLookup` by providing some title and optionally an author;
2. call `getMatches()`, which returns **the first 20 results** from GoodReads as a list of `GoodReadsSearchResult`;
3. select one or more results that interest you based on some criteria (or user input);
4. call `getMetadata()` from any `GoodReadsSearchResult` that interests you.

Here is an example:
```kotlin
// [1] create a lookup, here on a partial title
val lookup = GoodReadsLookup(title = "Billy Milligan")
// [2] fetch the 20 best results
val results = lookup.getMatches()
// [3] select some results, here based on a partial Author name
val myBook = results.firstOrNull { match ->
    match.authors.any { it.contains("Keyes") }
}
// [4] actually fetch the metadata on the result (if any)
val meta = myBook?.getMetadata()
meta?.let { println(it.toCompilableString()) } // use a prettier toString for console logging
```

The result of the above snippet is:
```
GoodReadsMetadata(
  title="The Minds of Billy Milligan",
  authors=listOf("Daniel Keyes"),
  url="https://www.goodreads.com/book/show/1391817.The_Minds_of_Billy_Milligan",
  id="1391817",
  isbn="9780394519432",
  pages=374,
  pubDate=LocalDate.parse("1981-10-01"),
)
```

By default, `getMatches()` will search in all fields if an author is provided, and in title only if the author is null. 
It is possible to change this behavior by setting `includeAuthorInSearch = true`.
See also [better searches](#better-searches-title-only).

### Get metadata automatically

`GoodReadsLookup` also provides a convenient method to try to find the right book automatically in the list of results:
```kotlin
GoodReadsLookup(title="je pense trop").findBestMatch().getMetadata()
//GoodReadsMetadata(
//    title="Je pense trop : comment canaliser ce mental envahissant",
//    authors=listOf("Christel Petitcollin"),
//    url="https://www.goodreads.com/book/show/10605863-je-pense-trop",
//    id="10605863",
//    isbn="9782813201966",
//    pages=252,
//    pubDate=LocalDate.parse("2010-11-22"),
//)

```
Or more directly using `GoodReadsMetadata.lookup` (exact same):
```kotlin
GoodReadsMetadata.lookup(title="Freakonomics", author="Steven Levitt, Stephen Dubner")
//GoodReadsMetadata(
//    title="Freakonomics: A Rogue Economist Explores the Hidden Side of Everything",
//    authors=listOf("Steven D. Levitt", "Stephen J. Dubner"),
//    url="https://www.goodreads.com/book/show/1202.Freakonomics",
//    id="1202",
//    isbn="9780061234002",
//    pages=268,
//    pubDate=LocalDate.parse("2005-04-12"),
//)
```

The library will first do a search (see `GoodReadsLookup.getMatches()`), 
then try to find a match in the list of results, throwing a `GrNotFoundException` if none. 

**IMPORTANT** for a result to be a match, the following conditions must apply:

- the title must match exactly (*),
- the author(s) must all be present in the list of authors, in the correct order (initials ignored)

(*) All comparisons are diacritics, symbols, space and casing insensitive.

Note that in case titles contain subtitles ("Some Title**:** some subtitle"), there will be a match either if the given title contains
the full title+subtitle, or only the title.

See also [better searches](#better-searches-title-only).

### Get all results (not only the first page)

Sometimes, you want to see more than the first 20 results of a GoodReads search.
`GoodReadsPaginatedSearchResult` is here for that.
Either instantiate it directly from a GoodReads search page URL (see also `GoodReadsUrl`), or use `GoodReadsLookup.getAllMatchesPaginated`.

Once instantiated, you can use `hasNext()` and `next()` in order to fetch more pages.
You can get the full list of already fetched search results using `allResults()`,
The total number of results and pages, as well as the last page fetched are available through
`totalResults`, `totalPages` and `currentPage` (the latter starting at `1`).
If no result is found, they will be set to `0`.

*NOTE*: `GoodReadsPaginatedSearchResult` does network calls inside the constructor, so ensure you instantiate
it in a background task on Android.

Example:
```kotlin
val paginatedResults: GoodReadsPaginatedSearchResults = GoodReadsLookup("how time war").getMatchesPaginated()
println("Total pages available: ${paginatedResults.totalPages}")

while(paginatedResults.hasNext()) {
    val nextResults = paginatedResults.next()
    // do something with the newest results, e.g. add them to an adapter on Android
}

paginatedResults.allResults() // here we have the complete list of results
```

### Other

The class `GoodReadsUrl` offers convenient static methods to construct the search query URL from title and/or author,
get the URL of a book given its GoodReads ID, etc. Check it out !

## Tips and tricks

### Better searches: title only

In general, GoodReads results are usually better when searching in title only.
Hence, we highly recommend to use title only in `GoodReadsLookup` if your title is specific enough,
and to only include author in search in cases where the title very short / generic.

If you are using `GoodReadsMetadata.lookup()` or `GoodReadsLookup.findBestMatch()`,
we recommend you pass the author (if known) since it is used for matching, but to disable author in search
by passing `includeAuthorInSearch = false` as argument.

### Retry on server fault

GoodReads tends to be a bit unstable, and may raise HTTP 5XX once in a while.
To avoid your code crashing on such cases, The `Retry` class is here to help.
Simply instantiate a `Retry` with the `RetryConfiguration` you need, and wrap
your calls to GoodReads into a `retrier.run { }` block.

See the samples below for examples. 

@sample samples.findBookInteractively
@sample samples.findBookAutomatically
@sample samples.lookupWithRetry
