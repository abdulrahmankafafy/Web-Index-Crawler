import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.util.*;

public class InvertedIndex {
    private Map<String, Map<String, List<Integer>>> index; // ahmed is in file10  the position is 15

    public InvertedIndex() {
        index = new HashMap<>();
    }

    public void buildIndex(List<File> files) {
        for (File file : files) {

            try (Scanner scanner = new Scanner(file)) {
                int position = 1;
                while (scanner.hasNext()) {
                    String word = scanner.next().toLowerCase(); // AHMED= ahmed

                    // Check if the word is already present in the index
                    if (!index.containsKey(word)) {
                        index.put(word, new HashMap<>());
                    }
                    Map<String, List<Integer>> filePositionMap = index.get(word);

                    // Check if the file is already present in the word's index
                    if (!filePositionMap.containsKey(file.getName())) {
                        filePositionMap.put(file.getName(), new ArrayList<>());
                    }
                    List<Integer> positions = filePositionMap.get(file.getName());

                    // Add the position of the word in the file
                    positions.add(position);
                    position++;
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String, Double> search(String query) { // File's name, CosineSimilarity
        Map<String, Double> cosineSimilarityMap = new HashMap<>();
        String[] queryWords = query.toLowerCase().split("\\s+");
        Map<String, Double> queryVector = new HashMap<>(); // to stor the tf-idf for the query words
        double queryVectorMagnitude = 0.0;

        // Calculate tf-idf values for query words and query vector magnitude
        for (String queryWord : queryWords) {
            // Calculate term frequency (tf)
            double tf = 1 + Math.log10(Collections.frequency(Arrays.asList(queryWords), queryWord));

            // Calculate inverse document frequency (idf)
            double idf = Math.log10((double) index.size() / index.getOrDefault(queryWord, Collections.emptyMap()).size());

            // Calculate tf-idf value for the term
            double tfIdf = tf * idf;

            // Store the tf-idf value in the query vector
            queryVector.put(queryWord, tfIdf);

            // Calculate the squared magnitude of the query vector
            queryVectorMagnitude += Math.pow(tfIdf, 2);
        }

        queryVectorMagnitude = Math.sqrt(queryVectorMagnitude);

        // Calculate cosine similarity scores for files
        for (String word : index.keySet()) {
            double idf = Math.log10((double) index.size() / index.get(word).size());
            for (String fileName : index.get(word).keySet()) {
                List<Integer> positions = index.get(word).get(fileName);
                double tf = positions.size();
                double tfIdf = tf * idf;

                // Update cosine similarity score for the file
                if (!cosineSimilarityMap.containsKey(fileName)) {
                    cosineSimilarityMap.put(fileName, 0.0);
                }
                cosineSimilarityMap.put(fileName, cosineSimilarityMap.get(fileName) + queryVector.getOrDefault(word, 0.0) * tfIdf);
            }
        }

        // Normalize cosine similarity scores
        for (String fileName : cosineSimilarityMap.keySet()) {
            double fileVectorMagnitude = 0.0;
            for (String word : index.keySet()) {// iterates over all words to access their tfidf
                double idf = Math.log10((double) index.size() / index.get(word).size());
                List<Integer> positions = index.get(word).getOrDefault(fileName, Collections.emptyList());
                double tf = positions.size();
                double tfIdf = tf * idf;

                // Calculate the squared magnitude of the file vector
                fileVectorMagnitude += Math.pow(tfIdf, 2);
            }
            fileVectorMagnitude = Math.sqrt(fileVectorMagnitude);

            // Calculate the cosine similarity between the query vector and the file vector
            double cosineSimilarity = cosineSimilarityMap.get(fileName) / (fileVectorMagnitude * queryVectorMagnitude);

            // Update the cosine similarity score for the file
            cosineSimilarityMap.put(fileName, cosineSimilarity);
        }

        return cosineSimilarityMap;
    }

    public void getPageLinks(String URL, int depth) {
        // Check if you have already crawled the URLs (not checking for duplicate content intentionally)
        if (!index.containsKey(URL) && depth > 0) {
            try {
                // If not, add it to the index
                index.put(URL, new HashMap<>());
                System.out.println("Crawling: " + URL);

                // Fetch the HTML code
                Document document = Jsoup.connect(URL).get();

                // Parse the HTML to extract links to other URLs
                Elements linksOnPage = document.select("a[href]");

                // For each extracted URL, go back to Step 1 (recursive call) with reduced depth
                for (Element page : linksOnPage) {
                    String nextURL = page.attr("abs:href");
                    getPageLinks(nextURL, depth - 1);

                    // Update the index for the current URL
                    Map<String, List<Integer>> filePositionMap = index.get(URL);
                    if (!filePositionMap.containsKey(nextURL)) {
                        filePositionMap.put(nextURL, new ArrayList<>());
                    }
                    List<Integer> positions = filePositionMap.get(nextURL);

                    // Add a special position marker (-1) for crawled URLs
                    positions.add(-1);
                }
            } catch (IOException e) {
                System.err.println("For '" + URL + "': " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        // Create a list of files
        List<File> files = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            files.add(new File("file" + i + ".txt"));
        }

        // Create an instance of InvertedIndex
        InvertedIndex invertedIndex = new InvertedIndex();

        // Build the index from the files
        invertedIndex.buildIndex(files);

        // Prompt the user for a query
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter query: ");
        String query = scanner.nextLine();

        // Search for the query in the index and retrieve cosine similarity scores
        Map<String, Double> cosineSimilarityMap = invertedIndex.search(query);

        // Sort the files based on cosine similarity scores in descending order
        List<Map.Entry<String, Double>> fileList = new ArrayList<>(cosineSimilarityMap.entrySet());
        fileList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));

        // Print the ranking of files based on cosine similarity scores
        System.out.println("Ranking of files based on cosine similarity:");
        for (Map.Entry<String, Double> entry : fileList) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }

        // Prompt the user to crawl a web page
        System.out.print("Enter URL to crawl: ");
        String url = scanner.nextLine();
        System.out.print("Enter the crawling depth: ");
        int depth = scanner.nextInt();

        // Crawl the web page and update the index
        invertedIndex.getPageLinks(url, depth);

        // Print the inverted index after crawling
        System.out.println("Inverted index after crawling: " + invertedIndex.index);
    }
}
