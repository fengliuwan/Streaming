package com.fengliuwan.venus.dao;

import com.fengliuwan.venus.entity.db.Item;
import com.fengliuwan.venus.entity.db.ItemType;
import com.fengliuwan.venus.entity.db.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.*;

/** an object of favoriteDao saves users favorite item into database */

@Repository
public class FavoriteDao {

    @Autowired
    private SessionFactory sessionFactory;

    /**
     * insert a favorite record into the database
     * @param userId    user's userId
     * @param item  item to add to user's favorite items list
     */
    public void setFavoriteItem(String userId, Item item) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            user.getItemSet().add(item);
            session.beginTransaction();
            session.save(user);
            session.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            session.getTransaction().rollback();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * remove a favorite item from user's favorite item list
     * @param userId    userId from front end
     * @param itemId    itemId of the item to remove from user's favorite list
     */
    public void unsetFavoriteItems(String userId, String itemId) {
        Session session = null;

        try {
            session = sessionFactory.openSession();
            User user = session.get(User.class, userId);
            Item item = session.get(Item.class, itemId);
            user.getItemSet().remove(item);
            session.beginTransaction();
            session.update(user);
            session.getTransaction().commit();
        } catch (Exception ex) {
            ex.printStackTrace();
            session.getTransaction().rollback();
        } finally {
            if (session != null) session.close();
        }
    }

    /**
     *
     * @param userId  userId of the user to get favorite items
     * @return items that the user likes
     */
    public Set<Item> getFavoriteItems(String userId) {
        try (Session session = sessionFactory.openSession()) {
            return session.get(User.class, userId).getItemSet();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new HashSet<>();
    }

    /**
     * Get favorite item ids for the given user
     * @param userId
     * @return a set of ids of items favorited by the given user
     */
    public Set<String> getFavoriteItemIds(String userId) {
        // 1 - to get gameId and sort by count
        // 2 - avoid recommending items already liked by user
        Set<String> favoriteItemsIds = new HashSet<>();
        try (Session session = sessionFactory.openSession()) {
            Set<Item> favoriteItems = session.get(User.class, userId).getItemSet();
            for (Item item : favoriteItems) {
                favoriteItemsIds.add(item.getId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("failed to get favorite item ids from database");
        }
        return favoriteItemsIds;
    }

    /**
     * organize favorite itemIds from our database of the given user to three sets of gameIds mapped to three types
     * The returned map includes three entries like
     * {"Video": [item1, item2, item3], "Stream": [item4, item5, item6], "Clip": [item7, item8, ...]}
     * @param favoriteItemIds
     * @return
     */
    // we can also use userId as argument and getItemSet(), then create HashMap
    public Map<String, List<String>> getFavoriteGameIds(Set<String> favoriteItemIds) {
        Map<String, List<String>> itemMap = new HashMap<>();
        // create map of itemtype (stream, video, clip) - list of <gameId>
        for (ItemType type : ItemType.values()){
            itemMap.put(type.toString(), new ArrayList<>());
        }
        try (Session session  = sessionFactory.openSession()) {
            for (String itemId : favoriteItemIds) {
                Item item = session.get(Item.class, itemId);
                // convert type to string!!!
                // itemid --> get item from DB --> get gameId
                // List<String> is gameIds, may have duplicates, used for sorting
                itemMap.get(item.getType().toString()).add(item.getGameId());
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return itemMap;
    }
}
