--name: insert-selfie-badge<!
REPLACE INTO selfie_badge (id, creator_id, name, description, criteria, image, tags, issuable_from_gallery, deleted, ctime, mtime)
VALUES (:id, :creator_id, :name, :description, :criteria, :image, :tags, :issuable_from_gallery, 0, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: update-selfie-badge!
UPDATE selfie_badge SET name = :name, description = :description, criteria = :criteria, image = :image, tags = :tags, issuable_from_gallery= :issuable_from_gallery, mtime = UNIX_TIMESTAMP()
WHERE id = :id AND creator_id = :creator_id

--name: soft-delete-selfie-badge!
UPDATE selfie_badge SET deleted = 1, mtime = UNIX_TIMESTAMP() WHERE id = :id AND creator_id = :creator_id

--name: hard-delete-selfie-badge!
DELETE FROM selfie_badge WHERE id = :id AND creator_id = :creator_id

--name: get-user-selfie-badges
SELECT * FROM selfie_badge WHERE creator_id = :creator_id AND deleted = 0
GROUP BY mtime DESC

--name: get-selfie-badge
SELECT * FROM selfie_badge WHERE id = :id

--name: get-selfie-badge-creator
SELECT creator_id FROM selfie_badge WHERE id = :id

--name: get-assertion-json
SELECT assertion_json FROM user_badge WHERE id = :id AND deleted = 0

--name: select-badge-id-by-user-badge-id
SELECT badge_id FROM user_badge WHERE id = :user_badge_id

--name: select-badge-tags
SELECT tag FROM badge_content_tag WHERE badge_content_id = :id

--name: select-criteria-content-by-badge-id
SELECT cc.id, cc.language_code, cc.markdown_text, cc.url
FROM criteria_content AS cc
LEFT JOIN badge_criteria_content AS bcc ON cc.id = bcc.criteria_content_id
WHERE bcc.badge_id = :badge_id

--name: update-badge-criteria-url!
UPDATE criteria_content SET url = :url WHERE id = :id

--name: finalise-issued-user-badge!
UPDATE user_badge SET
issuer_id = :issuer_id,
assertion_url = :assertion_url,
assertion_json = :assertion_json,
selfie_id = :selfie_id
WHERE id = :id

--name: get-issuer-information
SELECT * FROM issuer_content WHERE id = :id

--name: get-criteria-page-information
SELECT bc.name, bc.image_file, bc.description, cc.id AS id, cc.markdown_text AS criteria_content
FROM badge AS badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id)
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id)
WHERE cc.url = :url

--name: select-selfie-badge-issuing-history
SELECT ub.id, ub.user_id, ub.issued_on, ub.expires_on, ub.status, ub.revoked, u.first_name, u.last_name, u.profile_picture
FROM user_badge ub
JOIN user u ON (u.id=ub.user_id)
WHERE ub.selfie_id = :selfie_id AND ub.issuer_id = :issuer_id
ORDER BY ub.issued_on DESC
--GROUP BY ub.id

--name: select-multi-language-badge-content
--get badge by id
SELECT
badge.id as badge_id, badge.default_language_code,
bbc.badge_content_id,
bc.language_code,
bc.name, bc.description,
bc.image_file AS image,
ic.id AS issuer_content_id,
ic.url AS issuer_url,
cc.id AS criteria_content_id, cc.url AS criteria_url, cc.markdown_text AS criteria_content
FROM badge AS badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id)
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id)
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id AND bc.language_code = cc.language_code AND ic.language_code = cc.language_code)
WHERE badge.id = :id
GROUP BY badge.id, bc.language_code, cc.language_code, ic.language_code, bbc.badge_content_id

--name: revoke-issued-selfie-badge!
UPDATE user_badge SET revoked = 1, mtime = UNIX_TIMESTAMP() WHERE id= :id AND issuer_id = :issuer_id

--name: select-selfie-issuer-by-badge-id
SELECT issuer_id FROM user_badge WHERE id=:id

--name: select-issued-badge-validity-status
SELECT id, revoked, deleted, assertion_url FROM user_badge WHERE id=:id

--name:check-badge-issuable
SELECT sb.issuable_from_gallery, ub.selfie_id FROM user_badge ub
JOIN selfie_badge sb ON sb.id = ub.selfie_id
WHERE ub.gallery_id = :id AND ub.deleted = 0
ORDER BY ub.ctime DESC

--name: delete-selfie-badges-all!
DELETE FROM selfie_badge WHERE creator_id = :user_id

--name: select-latest-selfie-badges
SELECT ub.gallery_id, g.badge_id, g.badge_name AS name, g.badge_image AS image_file, sb.id AS selfie_id, ub.user_id
FROM gallery g
INNER JOIN user_badge ub ON g.id = ub.gallery_id
INNER JOIN selfie_badge sb ON ub.selfie_id = sb.id
WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
      AND ub.visibility != 'private' AND ub.selfie_id IS NOT NULL AND sb.issuable_from_gallery = 1 AND sb.deleted = 0 AND sb.creator_id != :user_id
GROUP BY g.badge_name, selfie_id, g.badge_image
ORDER BY sb.mtime DESC
LIMIT 1000

--name: select-user-gallery-ids
SELECT gallery_id, selfie_id FROM user_badge WHERE user_id = :user_id AND status != 'declined' AND revoked = 0 AND selfie_id IS NOT NULL

--name: select-issuable-gallery-badges
SELECT ub.gallery_id, ub.selfie_id FROM user_badge ub
LEFT JOIN selfie_badge sb ON sb.id = ub.selfie_id
WHERE ub.gallery_id IN (:gallery_ids) AND selfie_id IS NOT NULL
-- AND sb.issuable_from_gallery = 1

--name: insert-selfie-event<!
INSERT INTO social_event (subject, verb, object, type, ctime, mtime) VALUES (:subject, :verb, :object, 'selfie', UNIX_TIMESTAMP(), UNIX_TIMESTAMP())

--name: insert-selfie-event-owner!
INSERT INTO social_event_owners (owner, event_id) VALUES (:owner, :event_id)

--name: select-selfie-badge-receiver
SELECT user_id FROM user_badge WHERE id = :id

--name: select-issue-selfie-events
SELECT se.subject, se.verb, se.object, se.ctime, se.type, seo.event_id, seo.last_checked, u.first_name, u.last_name, u.profile_picture, u.profile_visibility,
    bc.name, bc.image_file, seo.hidden, ub.badge_id
FROM social_event_owners AS seo
INNER JOIN social_event AS se ON seo.event_id = se.id
INNER JOIN user_badge AS ub ON (ub.id = se.object)
INNER JOIN user AS u ON (ub.issuer_id = u.id)
INNER JOIN badge AS badge ON (badge.id = ub.badge_id)
INNER JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
INNER JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id AND bc.language_code = badge.default_language_code)
WHERE seo.owner = :user_id AND se.type = 'selfie' AND se.verb = 'issue' AND se.subject != :user_id
ORDER BY se.ctime DESC
LIMIT 1000

--name:check-badge-issued
SELECT id FROM user_badge WHERE status != 'declined' AND revoked = 0 AND selfie_id = :id

--name: created-badges-count
SELECT COUNT(DISTINCT id) AS count FROM selfie_badge WHERE deleted = 0;

--name: issued-badges-count
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND revoked = 0 AND status != 'declined' AND selfie_id IS NOT NULL;

--name: created-badges-count-after-date
SELECT COUNT(DISTINCT id) AS count FROM selfie_badge WHERE deleted = 0 AND ctime > :time;

--name: issued-badges-count-after-date
SELECT COUNT(DISTINCT id) AS count FROM user_badge WHERE deleted = 0 AND revoked = 0 AND status != 'declined' AND selfie_id IS NOT NULL AND ctime > :time;

--name: select-selfie-ids-badge
SELECT DISTINCT id FROM selfie_badge WHERE deleted = 0 AND name like :badge
ORDER BY ctime DESC
LIMIT 100000;

--name: select-selfie-ids-creator
SELECT DISTINCT sb.id FROM selfie_badge sb
INNER JOIN user u ON sb.creator_id = u.id
WHERE sb.deleted = 0  AND u.deleted = 0 AND CONCAT(u.first_name, ' ', u.last_name) LIKE :creator
ORDER BY sb.ctime DESC
LIMIT 100000;

--name: select-total-selfie-count
SELECT COUNT(DISTINCT sb.id) AS total
FROM selfie_badge sb
INNER JOIN user u ON sb.creator_id = u.id
WHERE (:country = 'all' OR u.country = :country);

--name: select-selfie-badges-all
SELECT sb.id, sb.name, sb.creator_id, sb.description, sb.criteria, sb.image, sb.tags, CONCAT(u.first_name, ' ', u.last_name)  AS creator_name, sb.ctime, sb.mtime, CAST(COUNT(DISTINCT ub.user_id) AS UNSIGNED) AS recipients
FROM selfie_badge sb
INNER JOIN user u ON sb.creator_id = u.id
INNER JOIN user_badge ub ON ub.selfie_id = sb.id
WHERE (:country = 'all' OR u.country = :country) AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
GROUP BY sb.id
ORDER BY
 CASE WHEN :order='name'  THEN sb.name END,
 CASE WHEN :order='creator' THEN creator_name END,
 CASE WHEN :order='recipients' THEN COUNT(DISTINCT ub.user_id) END DESC,
 CASE WHEN :order='mtime' THEN MAX(sb.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-selfie-badges-filtered
SELECT sb.id, sb.name, sb.creator_id, sb.description, sb.criteria, sb.image, sb.tags, CONCAT(u.first_name, ' ', u.last_name)  AS creator_name, sb.ctime, sb.mtime, COUNT(DISTINCT ub.user_id) AS recipients
FROM selfie_badge sb
INNER JOIN user u ON sb.creator_id = u.id
INNER JOIN user_badge ub ON ub.selfie_id = sb.id
WHERE (:country = 'all' OR u.country = :country) AND sb.id IN (:selfies) AND ub.status = 'accepted' AND ub.deleted = 0 AND ub.revoked = 0 AND (ub.expires_on IS NULL OR ub.expires_on > UNIX_TIMESTAMP())
GROUP BY sb.id
ORDER BY
 CASE WHEN :order='name'  THEN sb.name END,
 CASE WHEN :order='creator' THEN creator_name END,
 CASE WHEN :order='recipients' THEN COUNT(DISTINCT ub.user_id) END DESC,
 CASE WHEN :order='mtime' THEN MAX(sb.ctime) END DESC
LIMIT :limit OFFSET :offset

--name: select-selfie-countries
SELECT country FROM user AS u
               LEFT JOIN selfie_badge AS sb ON sb.creator_id = u.id
               GROUP BY country
               ORDER BY country
