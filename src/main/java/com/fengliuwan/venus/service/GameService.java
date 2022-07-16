package com.fengliuwan.venus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fengliuwan.venus.entity.db.Item;
import com.fengliuwan.venus.entity.db.ItemType;
import com.fengliuwan.venus.entity.response.Game;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/** call Twitch API */
@Service
public class GameService {

    private static final String TOKEN = "Bearer 53pkbu8v2oz85pf5evafhapuv5ayjo";
    private static final String CLIENT_ID = "a95jntyqhtv376wsqb1cl0bh7y4o2l";
    private static final String TOP_GAME_URL = "https://api.twitch.tv/helix/games/top?first=%s";
    private static final String GAME_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/games?name=%s";
    private static final int DEFAULT_GAME_LIMIT = 20;

    private static final String STREAM_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/streams?game_id=%s&first=%s";
    private static final String VIDEO_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/videos?game_id=%s&first=%s";
    private static final String CLIP_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/clips?game_id=%s&first=%s";
    private static final String TWITCH_BASE_URL = "https://www.twitch.tv/"; // for creating streaming url
    private static final int DEFAULT_SEARCH_LIMIT = 20;

    /* one method to generate url for top game search and specific game search */
    // Build the request URL which will be used when calling Twitch APIs,
    // e.g. https://api.twitch.tv/helix/games/top when trying to get top games.
    private String buildGameURL(String url, String gameName, int limit) {
        if (gameName.equals("")) {
            return String.format(url, limit);
        } else {
            try {
                /* Encode special characters in URL, e.g. Rick Sun -> Rick%20Sun */
                gameName = URLEncoder.encode(gameName, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }
        }
        return String.format(url, gameName);
    }

    // Similar to buildGameURL, build Search URL that will be used when calling Twitch API.
    // e.g. https://api.twitch.tv/helix/clips?game_id=12924.
    private String buildSearchURL(String url, String gameId, int limit) {
        try {
            gameId = URLEncoder.encode(gameId, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return String.format(url, gameId, limit);
    }

    /** get response to this url from Twitch,
     * return JSON format String of the JSONObject in the JSONArray paired with "data" field in the response */
    /** Send HTTP request to Twitch Backend based on the given URL,
     * and @return the body of the HTTP response returned from Twitch backend. */
    private String searchTwitch(String url) throws TwitchException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // Define the response handler to parse and return HTTP response body returned from Twitch
        // response is argument of the function in responseHandler interface,
        // interface has only one function that returns T, in our case String
        ResponseHandler<String> responseHandler = response -> {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                System.out.println("Response status: " + response.getStatusLine().getReasonPhrase());
                throw new TwitchException("Response code is not 200 while requesting data from Twitch API");
            }

            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new TwitchException("Failed to get result from Twitch API");
            }
            // convert response entity to JSONObject
            JSONObject object = new JSONObject(EntityUtils.toString(entity));
            // get value (which is a JSONArray) paired with "data" and convert JSONArray to string
            // JSONObject in JSONArray may have entry we do not need, so convert to String first and get info we need
            // object returned from Twitch： data needs to be mapped to JSONArray，otherwise throws JSONException
            return object.getJSONArray("data").toString();
        };

        try {
            /** Define the HTTP request, TOKEN and CLIENT_ID are used for user authentication on Twitch backend */
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", TOKEN);
            request.setHeader("Client-Id", CLIENT_ID);

            //responseHandler 需要的argument - HTTPResponse 在execute中得到并传入 以下为execute 部分source code
            // httpclient 收到request， execute得到response，把response交给Handler进行处理
            /**
             * final CloseableHttpResponse response = execute(target, request, context);
             * ...
             * final T result = responseHandler.handleResponse(response);
             * ...
             * return result;
             */
            return httpclient.execute(request, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to get result from Twitch API");
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /** Convert JSON format string data returned from Twitch to an Arraylist of Game objects
     * data returned is an array of Json format string */
    private List<Game> getGameList(String data){
        ObjectMapper mapper = new ObjectMapper();
        try {
            Game[] games = mapper.readValue(data, Game[].class);
            return Arrays.asList(games);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to processing game data returned from Twitch API");
        }
    }


    /** Integrate search() and getGameList() together, returns the top x popular games from Twitch.*/
    public List<Game> topGames(int limit){
        if (limit <= 0) {
            limit = DEFAULT_GAME_LIMIT;
        }
        String url = buildGameURL(TOP_GAME_URL, "", limit);
        String data = searchTwitch(url);
        return getGameList(data);
    }

    /** Integrate search() and getGameList() together, returns the dedicated game based on the game name.*/
    public Game searchGame(String gameName){
        String url = buildGameURL(GAME_SEARCH_URL_TEMPLATE, gameName, 0);
        String data = searchTwitch(url); // data is JsonArray of games matching gameName
        List<Game> gameList = getGameList(data);

        if (gameList.size() != 0) {
            return gameList.get(0); // return the first game matching the gamaName
        }
        return null;
    }

    /**
     * Similar to getGameList, convert the json data returned from Twitch to a list of Item objects.
     * @param data
     * @return
     */
    private List<Item> getItemList(String data) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Item[].class));
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            throw new RuntimeException();
        }
    }

    // Returns the top x streams based on game ID.
    private List<Item> searchStreams(String gameId, int limit) {
        String url = buildSearchURL(STREAM_SEARCH_URL_TEMPLATE, gameId, limit);
        String data = searchTwitch(url);
        List<Item> stream = getItemList(data);

        for (Item item : stream) {
            item.setUrl(TWITCH_BASE_URL + item.getBroadcasterName());
            item.setType(ItemType.STREAM);
        }

        return stream;
    }

    // Returns the top x clips based on game ID.
    private List<Item> searchClips(String gameId, int limit) {
        List<Item> clips = getItemList(searchTwitch(buildSearchURL(CLIP_SEARCH_URL_TEMPLATE, gameId, limit)));
        for (Item item : clips) {
            item.setType(ItemType.CLIP);
        }
        return clips;
    }

    // Returns the top x videos based on game ID.
    private List<Item> searchVideos(String gameId, int limit) {
        List<Item> videos = getItemList(searchTwitch(buildSearchURL(VIDEO_SEARCH_URL_TEMPLATE, gameId, limit)));
        for (Item item : videos) {
            item.setType(ItemType.VIDEO);
        }
        return videos;
    }

    /**
     *
     * @param gameId
     * @param type
     * @param limit
     * @return
     */
    // used by recommendation service, need to be protected or public
    public List<Item> searchByType(String gameId, ItemType type, int limit) {
        List<Item> items = new ArrayList<>();
        switch (type) {
            case STREAM:
                items = searchStreams(gameId, limit);
                break;
            case VIDEO:
                items = searchVideos(gameId, limit);
                break;
            case CLIP:
                items = searchClips(gameId, limit);
                break;
        }

        // Update gameId for all items. GameId is used by recommendation function
        for (Item item : items) {
            item.setGameId(gameId);
        }

        return items;
    }

    public Map<String, List<Item>> searchItems(String gameId) {
        Map<String, List<Item>> itemMap = new HashMap<>();
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.name(), searchByType(gameId, type, DEFAULT_GAME_LIMIT));
        }
        return itemMap;
    }
}
