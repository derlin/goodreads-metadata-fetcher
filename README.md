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

### Get metadata directly

```kotlin
GoodReadsMetadata.lookup("Animal Farm")
GoodReadsMetadata.lookup("Animal Farm", "George Orwell")
// => GoodReadsMetadata(url=https://www.goodreads.com/book/show/170448.Animal_Farm, id=170448, 
//        title=Animal Farm, authors=[George Orwell], isbn=9780451526342, pages=141, pubDate=1945-08-17)
```
This will first do a GoodReads search using the title, then try to find the book using title and author
(throwing a `GrNotFoundException` if no match is found).

**IMPORTANT** for a result to be a match, the following conditions must apply:

- the title must be an "exact match"
- the author(s) must all be present in the list of authors, in the correct order

All comparisons are diacritics, symbols, space and casing insensitive

### Get results first, metadata second

In case the way matches are found does not suit you, it is possible to first do a search, then fetch the metadata:
```kotlin
// Lookup partial title
val allMatches = GoodReadsLookup("Billy Milligan").getAllMatches()
val myMatch = allMatches.firstOrNull { match ->
    // Lookup partial author name
    match.authors.any { it.contains("Keyes") }
}
myMatch?.getMetadata()
// => GoodReadsMetadata(url=https://www.goodreads.com/book/show/1391817.The_Minds_of_Billy_Milligan, id=1391817, 
//        title=The Minds of Billy Milligan, authors=[Daniel Keyes], isbn=9780394519432, pages=374, pubDate=1981-10-01)
```
