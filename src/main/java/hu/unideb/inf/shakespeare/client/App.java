package hu.unideb.inf.shakespeare.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import hu.unideb.inf.shakespeare.client.api.BookmarkControllerApi;
import hu.unideb.inf.shakespeare.client.api.PlayControllerApi;

/**
 * A minimal client of the RESTful Shakespeare API. The {@code PlayControllerApi}
 * and {@code BookmarkControllerApi} interfaces are generated from the OpenAPI
 * description (see {@code pom.xml}): they are Spring HTTP interfaces, i.e.,
 * their methods are annotated with {@code @HttpExchange}, and Spring
 * implements them at runtime with a {@link RestClient}.
 */
public class App {

    private static final String BASE_URL = "http://localhost:8080";

    public static void main(String[] args) {
        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                // The credentials of a demo user, sent with every request
                .defaultHeaders(headers -> headers.setBasicAuth("alice", "wonderland"))
                .build();
        HttpServiceProxyFactory factory = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build();
        PlayControllerApi plays = factory.createClient(PlayControllerApi.class);
        BookmarkControllerApi bookmarks = factory.createClient(BookmarkControllerApi.class);

        // GET: the representation is XML in a String (the description gives no
        // schema for it); the links are in the Link header fields.
        ResponseEntity<String> scene = plays.scene("macbeth", 1, 1);
        System.out.println("GET a scene: " + scene.getStatusCode());
        scene.getHeaders().getOrEmpty(HttpHeaders.LINK).forEach(link -> System.out.println("  Link: " + link));
        System.out.println("  " + abbreviate(scene.getBody()));

        // POST (with authentication): a new bookmark.
        ResponseEntity<String> created = bookmarks.bookmarkScene("macbeth", 1, 1, "The witches");
        URI location = created.getHeaders().getLocation();
        System.out.println("POST a bookmark: " + created.getStatusCode() + ", Location: " + location);
        System.out.println("  " + created.getBody());

        // DELETE: the generated method needs the user and the identifier of the
        // bookmark, not its URI, so the client has to take the URI apart.
        String path = location.getPath();
        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        System.out.println("DELETE the bookmark: " + bookmarks.delete("alice", id).getStatusCode());

        // Error responses (4xx) are thrown as exceptions, with the problem
        // details in the response body.
        try {
            plays.play("othello");
        } catch (HttpClientErrorException e) {
            System.out.println("GET a nonexistent play: " + e.getStatusCode());
            System.out.println("  " + e.getResponseBodyAsString());
        }
    }

    private static String abbreviate(String text) {
        return text.length() <= 200 ? text : text.substring(0, 200) + "...";
    }

}
