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

--name: update-user-badge-assertions!
UPDATE user_badge SET assertion_url = :assertion_url, assertion_json = :assertion_json
WHERE id = :id

--name: get-assertion-json
SELECT assertion_json FROM user_badge WHERE id = :id AND deleted = 0

--name: select-badge-id-by-user-badge-id
SELECT badge_id FROM user_badge WHERE id = :user_badge_id

--name: select-badge-tags
SELECT tag FROM badge_content_tag WHERE badge_content_id = :id

--name: select-criteria-content-id-by-badge-id
SELECT criteria_content_id FROM badge_criteria_content WHERE badge_id = :badge_id

--name: update-badge-criteria-url!
UPDATE criteria_content SET url = :url WHERE badge_id = :badge_id

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
--(SELECT tag FROM badge_content_tag WHERE badge_content_id = bbc.badge_content_id) AS tags
FROM badge AS badge
JOIN badge_badge_content AS bbc ON (bbc.badge_id = badge.id)
JOIN badge_content AS bc ON (bc.id = bbc.badge_content_id)
JOIN badge_issuer_content AS bic ON (bic.badge_id = badge.id)
JOIN issuer_content AS ic ON (ic.id = bic.issuer_content_id)
--LEFT JOIN badge_content_tag AS bct ON (bc.id = bct.badge_content_id)
JOIN badge_criteria_content AS bcc ON (bcc.badge_id = badge.id)
JOIN criteria_content AS cc ON (cc.id = bcc.criteria_content_id AND bc.language_code = cc.language_code AND ic.language_code = cc.language_code)
WHERE badge.id = :id
GROUP BY badge.id, bc.language_code, cc.language_code, ic.language_code, bbc.badge_content_id
