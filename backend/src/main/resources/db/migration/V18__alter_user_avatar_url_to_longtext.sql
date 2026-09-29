-- V18: Alter user avatar_url to LONGTEXT for custom photos and high-res avatars
ALTER TABLE users MODIFY COLUMN avatar_url LONGTEXT NULL;
