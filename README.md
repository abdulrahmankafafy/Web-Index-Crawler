# WebIndexCrawler

The `WebIndexCrawler` project is a Java application that builds an inverted index from a collection of text files and allows searching for query terms using cosine similarity. Additionally, it includes a web crawler that indexes URLs.

## Features

- Build an inverted index from a list of text files.
- Calculate and search for query terms using cosine similarity.
- Crawl web pages and update the index with URLs.

## Usage

### Building the Index

1. Create a list of text files to be indexed.
2. Use the `buildIndex` method to create the inverted index.

### Searching the Index

1. Prompt the user for a query.
2. Use the `search` method to retrieve cosine similarity scores for the query terms.

### Crawling Web Pages

1. Prompt the user for a URL and crawling depth.
2. Use the `getPageLinks` method to crawl the web page and update the index with the URLs found.
