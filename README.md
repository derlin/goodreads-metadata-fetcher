# GoodReads Metadata Fetcher

This Kotlin Library implements a basic metadata lookup for [GoodReads](https://www.goodreads.com/).

## Installation

TODO: publish gradle package

### Build the library

After cloning the repo, run the following to install the package on your local maven repo:

```shell
./gradlew install
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
myBook?.getMetadata()
```

The result of the above snippet is:
```
GoodReadsMetadata(
    url = "https://www.goodreads.com/book/show/1391817.The_Minds_of_Billy_Milligan",
    id = "1391817",
    title = "The Minds of Billy Milligan",
    authors = ["Daniel Keyes"],
    isbn = "9780394519432",
    pages = 374,
    pubDate = "1981-10-01" // as a LocalDate
)
```

By default, `getMatches()` will search in all fields if an author is provided, and in title only if the author is null. 
It is possible to change this behavior by setting `includeAuthorInSearch = true`.
See also [better searches](#better-searches-title-only).

### Get metadata automatically

`GoodReadsLookup` also provides a convenient method to try to find the right book automatically in the list of results:
```kotlin
GoodReadsLookup(title = "Animal Farm", author = "George Orwell").findBestMatch()
// => GoodReadsMetadata(url=https://www.goodreads.com/book/show/170448.Animal_Farm, id=170448, 
//        title=Animal Farm, authors=[George Orwell], isbn=9780451526342, pages=141, pubDate=1945-08-17)
```
Or more directly using `GoodReadsMetadata.lookup` (exact same):
```kotlin
GoodReadsMetaData.lookup(title = "Animal Farm", author = "George Orwell")
// => GoodReadsMetadata(url=https://www.goodreads.com/book/show/170448.Animal_Farm, id=170448, 
//        title=Animal Farm, authors=[George Orwell], isbn=9780451526342, pages=141, pubDate=1945-08-17)
```

The library will first do a search (see `GoodReadsLookup.getAllMatches()`), 
then try to find a match in the list of results, throwing a `GrNotFoundException` if none. 

**IMPORTANT** for a result to be a match, the following conditions must apply:

- the title must match exactly (*),
- the author(s) must all be present in the list of authors, in the correct order (initials ignored)

(*) All comparisons are diacritics, symbols, space and casing insensitive.

Note that in case titles contain subtitles ("Some Title**:** some subtitle"), there will be a match either if the given title contains
the full title+subtitle, or only the title.

See also [better searches](#better-searches-title-only).

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