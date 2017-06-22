--name: select-uids-emails-by-emails
SELECT user_id, email FROM user_email WHERE verified = 1 AND email IN (:emails);

--name: select-backback-emails-by-uids
SELECT user_id, email FROM user_email WHERE user_id IN (:user_ids) ORDER BY user_id, backpack_id IS NULL, primary_address DESC, ctime;

--name: insert-pending-badge-for-email!
INSERT INTO pending_factory_badge (assertion_url, email, ctime) VALUES (:assertion_url, :email, UNIX_TIMESTAMP())

--name: select-pending-badges-by-user
SELECT DISTINCT p.assertion_url, p.email FROM pending_factory_badge AS p
       INNER JOIN user_email AS ue ON p.email = ue.email
       WHERE assertion_url IS NOT NULL AND ue.user_id = :user_id AND ue.verified = 1

--name: delete-duplicate-pending-badges!
DELETE FROM pending_factory_badge WHERE email = :email AND assertion_url = :assertion_url

--name: select-badge-updates
-- FIXME (evidence_url)
SELECT ub.id, ub.user_id, ub.email, ub.assertion_url, ub.mtime, ube.url AS evidence_url, ub.rating FROM user_badge AS ub
       LEFT JOIN user_badge_evidence AS ube ON (ube.user_badge_id = ub.id)
       WHERE ub.status = 'accepted' AND ub.deleted = 0 AND ub.user_id = :user_id AND ub.id = :id

