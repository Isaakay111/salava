--name: select-users-for-report
SELECT u.id, u.profile_picture, u.profile_visibility, u.ctime, CONCAT(u.first_name, " ", u.last_name) AS name, u.activated, CAST(COUNT(DISTINCT ub.id) AS UNSIGNED) AS badge_count
FROM user u
LEFT JOIN user_badge ub ON ub.user_id = u.id
WHERE u.id IN (:ids)
GROUP BY u.id

--name: select-user-ids-badge
SELECT DISTINCT ub.user_id, (SELECT COUNT(DISTINCT gallery_id) FROM user_badge WHERE gallery_id IN (:badge_ids) AND user_id = ub.user_id) AS count FROM user_badge ub
INNER JOIN user_space us ON us.user_id = ub.user_id
WHERE ub.gallery_id IN (:badge_ids) AND ub.issued_on BETWEEN IFNULL(:from, (SELECT MIN(issued_on) FROM user_badge) ) AND IFNULL(:to, (SELECT MAX(issued_on) FROM user_badge))
      AND ub.deleted = 0 AND ub.revoked = 0 AND us.space_id = :space_id
GROUP BY ub.user_id, count
HAVING count = :expected_count
ORDER BY ub.issued_on DESC
LIMIT 100000

--name: select-badge-ids-report
SELECT DISTINCT ub.gallery_id FROM user_badge ub
JOIN user u ON u.id = ub.user_id
WHERE ub.user_id IN (:ids) AND ub.issued_on BETWEEN IFNULL(:from, (SELECT MIN(issued_on) FROM user_badge) ) AND IFNULL(:to, (SELECT MAX(issued_on) FROM user_badge))
GROUP BY ub.gallery_id
LIMIT 100000

--name: select-user-badges-report
SELECT ub.id, ub.visibility, ub.status, ub.expires_on, ub.issued_on, g.badge_name, g.issuer_name, g.badge_image
FROM user_badge ub
JOIN gallery g ON g.id = ub.gallery_id
WHERE ub.deleted = 0 AND ub.revoked = 0 AND g.id IN (:ids)
GROUP BY ub.id, ub.visibility, ub.status, ub.issued_on
ORDER BY ub.issued_on DESC

--name: select-user-ids-space-report
SELECT DISTINCT(us.user_id) FROM user_space us WHERE us.space_id = :space_id
