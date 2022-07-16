package com.fengliuwan.venus.service;

import com.fengliuwan.venus.dao.FavoriteDao;
import com.fengliuwan.venus.entity.db.Item;
import com.fengliuwan.venus.entity.db.ItemType;
import com.fengliuwan.venus.entity.response.Game;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    @Autowired
    private FavoriteDao favoriteDao;

    @Autowired
    private GameService gameService;

    private static final int DEFAULT_GAME_LIMIT = 3;
    private static final int DEFAULT_PER_GAME_RECOMMENDATION_LIMIT = 10;
    private static final int DEFAULT_TOTAL_RECOMMENDATION_LIMIT = 20;

    /**
     * Return a map of Item objects as the recommendation result.
     * Keys of the map are [Stream, Video, Clip].
     * Each key is corresponding to a list of Items objects,
     * each item object is a recommended item based on the previous favorite records by the user.
     * @param userId
     * @return
     * @throws RecommendationException
     */
    public Map<String, List<Item>> recommendItemsByUser(String userId) throws RecommendationException {
        // return map of type - list of items (different items may have the same gameId, and may of different type)
        Map<String, List<Item>> recommendedItemMap = new HashMap<>();
        // get users favorite items
        Set<String> favoriteItemIds = favoriteDao.getFavoriteItemIds(userId);
        // type - list of gameIds
        Map<String, List<String>> favoriteGameIds = favoriteDao.getFavoriteGameIds(favoriteItemIds);

        for(Map.Entry<String, List<String>> entry : favoriteGameIds.entrySet()) {
            // for each type, if user did not like anything of this type, recommend top games
            if (entry.getValue().size() == 0) {
                List<Game> topGames;
                try {
                    // get top games from twitch api, convert Json string to list of games
                    topGames = gameService.topGames(DEFAULT_GAME_LIMIT);
                } catch (TwitchException ex) {
                    throw new RecommendationException("Failed to get game data for recommendation");
                }
                recommendedItemMap.put(entry.getKey(),
                        recommendByTopGames(ItemType.valueOf(entry.getKey()), topGames));
            } else {
                // if user has favorite gameIds for the type
                recommendedItemMap.put(entry.getKey(),
                        recommendByFavoriteHistory(favoriteItemIds, entry.getValue(), ItemType.valueOf(entry.getKey())));
            }
        }
        return recommendedItemMap;
    }

    /**
     * Return a map of Item objects as the recommendation result.
     * Keys of the map are [Stream, Video, Clip].
     * Each key is corresponding to a list of Items objects,
     * each item object is a recommended item based on the top games currently on Twitch.
     * @return
     * @throws RecommendationException
     */
    public Map<String, List<Item>> recommendItemsByDefault() throws RecommendationException {
        Map<String, List<Item>> itemMap = new HashMap<>();

        List<Game> topGames;
        try {
            // get top N games
            topGames = gameService.topGames(DEFAULT_GAME_LIMIT);
        } catch (TwitchException e) {
            throw new RecommendationException("failed to get top games from twitch in recommend items by default");
        }
        // for each type of data, search twitch for items matching top games
        for (ItemType type : ItemType.values()) {
            itemMap.put(type.toString(), recommendByTopGames(type, topGames));
        }
        return itemMap;
    }

    /**
     * Return a list of Item objects for the given type. Types are one of [Stream, Video, Clip].
     * Added items are related to the top games provided in the argument.
     * @param type
     * @param topGames
     * @return
     * @throws RecommendationException
     */
    private List<Item> recommendByTopGames(ItemType type, List<Game> topGames) throws RecommendationException {
        List<Item> recommendedItems = new ArrayList<>();
        for (Game game : topGames) {
            List<Item> items = null;
            try {
                //search twitch for items of given gameId of given type
                items = gameService.searchByType(game.getId(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException tex){
                throw new RecommendationException("Failed to get recommendation result");
            }
            for (Item item : items) {
                //每种类型 推荐最多的item个数
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    return recommendedItems;
                }
                recommendedItems.add(item);
            }
        }
        return recommendedItems;
    }


    /**
     * Return a list of Item objects for the given type. Types are one of [Stream, Video, Clip].
     * All items are related to the items previously favorited by the user.
     * E.g., if a user favorited some videos about game "Just Chatting",
     * then it will return some other videos about the same game.
     * @param favoriteItemIds itemIds of items already liked by user, used to avoid recommending items already liked
     * @param favoriteGameIds gameIds of items already liked by user, used to sort gameIds for most liked games
     * @param type
     * @return
     * @throws RecommendationException
     */
    private List<Item> recommendByFavoriteHistory(Set<String> favoriteItemIds,
                                                  List<String> favoriteGameIds,
                                                  ItemType type) throws RecommendationException {

        // Count the favorite game IDs from the database for the given user.
        // E.g. if the favorited game ID list is ["1234", "2345", "2345", "3456"], the returned Map is {"1234": 1, "2345": 2, "3456": 1}
        //    Map<String, Long> favoriteGameIdByCount = new HashMap<>();
        //    for(String gameId : favoritedGameIds) {
        //      favoriteGameIdByCount.put(gameId, favoriteGameIdByCount.getOrDefault(gameId, 0L) + 1);
        //    }
        // gameId -- count
        Map<String, Long> favoriteGameIdByCount = favoriteGameIds.parallelStream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // Sort the game Id by count.
        // E.g. if the input is {"1234": 1, "2345": 2, "3456": 1},
        // the returned Map is {"2345": 2, "1234": 1, "3456": 1}

        List<Map.Entry<String, Long>> sortedFavoriteGameIdListByCount =
                new ArrayList<>(favoriteGameIdByCount.entrySet());

        sortedFavoriteGameIdListByCount.sort((Map.Entry<String, Long> e1, Map.Entry<String, Long> e2)
                -> Long.compare(e2.getValue(), e1.getValue()));

        // See also: https://stackoverflow.com/questions/109383/sort-a-mapkey-value-by-values

        // only recommend top 3 liked games by user
        if (sortedFavoriteGameIdListByCount.size() > DEFAULT_GAME_LIMIT) {
            sortedFavoriteGameIdListByCount = sortedFavoriteGameIdListByCount.subList(0, DEFAULT_GAME_LIMIT);
        }

        List<Item> recommendedItems = new ArrayList<>();

        // Search Twitch based on the favorite game IDs returned in the above step.
        for (Map.Entry<String, Long> favoriteGame : sortedFavoriteGameIdListByCount) {
            List<Item> items = null;
            try {
                items = gameService.searchByType(favoriteGame.getKey(), type, DEFAULT_PER_GAME_RECOMMENDATION_LIMIT);
            } catch (TwitchException e) {
                throw new RecommendationException("Failed to get recommendation result");
            }
            // add items to recommendedItems
            for (Item item : items) {
                // for each type, recommend n items
                if (recommendedItems.size() == DEFAULT_TOTAL_RECOMMENDATION_LIMIT) {
                    return recommendedItems;
                }
                // avoid recommending items already liked by user
                if (!favoriteItemIds.contains(item.getId())) {
                    recommendedItems.add(item);
                }
            }
        }
        return recommendedItems;
    }
}
