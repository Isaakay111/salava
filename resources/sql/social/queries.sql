--name: insert-badge-message<!
--add new badge-message
INSERT INTO badge_message (badge_id, user_id, message, ctime, mtime)
                   VALUES (:badge_id, :user_id, :message, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: select-badge-messages
--get badge's messages
SELECT bm.id, bm.badge_id, bm.message, bm.ctime, bm.user_id, u.first_name, u.last_name, u.profile_picture FROM badge_message bm
       JOIN user AS u ON (u.id = bm.user_id)
       WHERE badge_id = :badge_id AND bm.deleted=0
       ORDER BY bm.ctime DESC

--name: select-badge-messages-limit
--get badge's messages
SELECT bm.id, bm.badge_id, bm.message, bm.ctime, bm.user_id, u.first_name, u.last_name, u.profile_picture FROM badge_message bm
       JOIN user AS u ON (u.id = bm.user_id)
       WHERE badge_id = :badge_id AND bm.deleted=0
       ORDER BY bm.ctime DESC
       LIMIT :limit OFFSET :offset


--name: select-badge-messages-count
--get badge's messages
SELECT ctime, user_id FROM badge_message WHERE badge_id = :badge_id AND deleted=0
       
--name: update-badge-message-deleted!
UPDATE badge_message SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :message_id

--name: select-badge-message-owner
SELECT user_id FROM badge_message where id = :message_id

--name: replace-badge-message-view!
REPLACE INTO badge_message_view (user_id, badge_id, mtime)
       VALUES (:user_id, :badge_id, UNIX_TIMESTAMP())

--name: select-badge-message-last-view
SELECT mtime FROM badge_message_view where badge_id = :badge_id AND user_id = :user_id


--name: insert-connect-badge<!
--add new connect with badge
INSERT IGNORE INTO social_connections_badge (user_id, badge_id, ctime)
                   VALUES (:user_id, :badge_id, UNIX_TIMESTAMP())

--name: delete-connect-badge!
DELETE FROM social_connections_badge WHERE user_id = :user_id  AND badge_id = :badge_id

--name: delete-connect-badge-by-badge-id!
DELETE FROM social_connections_badge WHERE user_id = :user_id  AND badge_id =  (SELECT badge_id from user_badge where badge_id = :badge_id AND user_id = :user_id) 

--name: select-connection-badge
SELECT badge_id FROM social_connections_badge WHERE user_id = :user_id AND badge_id = :badge_id

-- name: select-user-connections-badge
-- get users badge connections
SELECT DISTINCT badge.id, bc.name, bc.image_file, bc.description FROM social_connections_badge AS scb
     JOIN badge AS badge ON (badge.id =  scb.badge_id)
     JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
     JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
     WHERE scb.user_id = :user_id
     GROUP BY bc.id, bc.name, bc.image_file, bc.description
     ORDER BY bc.name ASC



--name: insert-social-event<!
INSERT INTO social_event (subject, verb, object, type, ctime, mtime) VALUES (:subject, :verb, :object, :type, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: insert-event-owners!
INSERT INTO social_event_owners (owner, event_id) VALUES (:owner, :event_id) 


--name: select-users-from-connections-badge
SELECT user_id AS owner from social_connections_badge where badge_id = :badge_id

--name: select-admin-users-id
SELECT id AS owner from user where role='admin';

--name: select-user-events
-- FIXME (content columns)
SELECT se.subject, se.verb, se.object, se.ctime, seo.event_id, seo.last_checked, bc.name, bc.image_file, seo.hidden FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     JOIN badge AS badge ON (badge.id = se.object)
     JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
     JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id) AND bc.language_code = badge.default_language_code
     JOIN social_connections_badge AS scb ON :user_id = scb.user_id
     WHERE owner = :user_id AND se.type = 'badge' AND se.object = scb.badge_id
     ORDER BY se.ctime DESC
     LIMIT 1000

--name: select-admin-events
SELECT  se.subject, se.verb, se.object, se.ctime, seo.event_id, seo.last_checked, seo.hidden, re.item_name, re.report_type FROM social_event_owners AS seo
     JOIN social_event AS se ON seo.event_id = se.id
     LEFT JOIN report_ticket AS re  ON se.object = re.id
     WHERE seo.owner = :user_id AND se.verb = 'ticket' AND se.type = 'admin' AND re.status = 'open'
     ORDER BY se.ctime DESC
     LIMIT 1000;



--name: select-user-new-messages
SELECT bmv.badge_id, bm.user_id, bm.message, bm.ctime, u.first_name, u.last_name, u.profile_picture, bmv.mtime AS last_viewed from badge_message_view AS bmv
       JOIN badge_message AS bm ON bmv.badge_id = bm.badge_id AND bm.deleted = 0
       JOIN user AS u ON (u.id = bm.user_id)
       where bmv.user_id = :user_id
       ORDER BY bm.ctime ASC




--name: select-messages-with-badge-id
SELECT bmv.badge_id, bm.user_id, bm.message, bm.ctime, u.first_name, u.last_name, u.profile_picture, bmv.mtime AS last_viewed from badge_message as bm
JOIN user AS u ON (u.id = bm.user_id)
JOIN badge_message_view AS bmv ON bm.badge_id = bmv.badge_id AND :user_id =  bmv.user_id
WHERE bm.badge_id IN (:badge_ids) AND bm.deleted = 0
ORDER BY bm.ctime DESC
LIMIT 100


--name: update-hide-user-event!
UPDATE social_event_owners SET hidden = 1 WHERE event_id = :event_id AND owner = :user_id


--name: select-badge-id-by-message-id
SELECT badge_id from badge_message where id = :message_id

--name: select-badge-id-by-user-badge-id
SELECT badge_id from user_badge where id = :user_badge_id

--name: select-user-badge-count
SELECT COUNT(*) AS count from user_badge where user_id = :user_id AND status = 'accepted' AND deleted=0

--name: select-user-profile-picture
SELECT profile_picture from user where id = :user_id

--name: select-user-not-verified-emails
SELECT email FROM user_email WHERE user_id = :user_id AND verified= 0;


--name: update-last-checked-user-event-owner!
UPDATE social_event_owners SET last_checked = UNIX_TIMESTAMP() WHERE event_id IN (:event_ids) AND owner = :user_id


