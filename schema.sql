-- Cyber-Hideout (Virtual Theater) Database Schema
-- Designed for PostgreSQL 15+ / Supabase
-- Enforces Privacy-First Absolute Isolation Rules

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

--------------------------------------------------------------------------------
-- 1. USERS TABLE
--------------------------------------------------------------------------------
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(50) UNIQUE NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    avatar_url TEXT,
    bio TEXT,
    -- App Rules: Profile status switches (Owner controls if follow counts are shown)
    show_follow_counts BOOLEAN DEFAULT TRUE NOT NULL,
    -- Default consent preference for citation: "Allow Citation (Anonymous)"
    allow_anonymous_citation_default BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS on users
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

-- Users can read any user profile (for screen display), but edit only their own
CREATE POLICY users_select_policy ON users
    FOR SELECT USING (true);

CREATE POLICY users_update_policy ON users
    FOR UPDATE USING (auth.uid() = id);

--------------------------------------------------------------------------------
-- 2. POSTS (DYNAMIC THREADS) TABLE
--------------------------------------------------------------------------------
CREATE TABLE posts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    author_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    content TEXT NOT NULL,
    media_url TEXT,
    -- Cached counts: Anyone can view total counts (Rule 1: "The Numbers Exist")
    likes_count INT DEFAULT 0 NOT NULL,
    comments_count INT DEFAULT 0 NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS on posts
ALTER TABLE posts ENABLE ROW LEVEL SECURITY;

-- Anyone authenticated can view all posts (Public square/Virtual Theater stage)
CREATE POLICY posts_select_policy ON posts
    FOR SELECT USING (true);

-- Users can insert, update, or delete only their own posts
CREATE POLICY posts_insert_policy ON posts
    FOR INSERT WITH CHECK (auth.uid() = author_id);

CREATE POLICY posts_update_delete_policy ON posts
    FOR ALL USING (auth.uid() = author_id);

--------------------------------------------------------------------------------
-- 3. FOLLOW RELATIONSHIPS
-- Rule 1: "The Numbers Exist, The Lists Do Not"
--------------------------------------------------------------------------------
CREATE TABLE follows (
    follower_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    following_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT no_self_follow CHECK (follower_id <> following_id)
);

-- Enable RLS on follows
ALTER TABLE follows ENABLE ROW LEVEL SECURITY;

-- CRITICAL PRIVACY ENFORCEMENT:
-- Only the owner of the list can select/view the detailed entries.
-- That is: User A can see who they follow, and who follows them,
-- but a random User C cannot query follower relationships of User A.
CREATE POLICY follows_select_policy ON follows
    FOR SELECT USING (
        auth.uid() = follower_id     -- Current user's following list (who I follow)
        OR auth.uid() = following_id -- Current user's followers list (who follows me)
    );

CREATE POLICY follows_insert_delete_policy ON follows
    FOR ALL USING (auth.uid() = follower_id);

--------------------------------------------------------------------------------
-- 4. PRIVATE 1-ON-1 COMMENTS ("THE BACKSTAGE CHAT")
-- Rule 2: "One-Way Transparent Comments (The Backstage Rule)"
-- Rule 3: "Dual-Consent Spotlight Mechanism (Public Citation)"
--------------------------------------------------------------------------------
CREATE TABLE comments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    post_id UUID REFERENCES posts(id) ON DELETE CASCADE NOT NULL,
    commenter_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    content TEXT NOT NULL,
    
    -- "The Spotlight Status":
    -- is_featured: Has the post author customized this comment as an anonymous board snippet?
    is_featured BOOLEAN DEFAULT FALSE NOT NULL,
    
    -- citizen_consent_given: Has the commenter given consent to be cited? (either pre-set or real-time popup approved)
    citizen_consent_given BOOLEAN DEFAULT FALSE NOT NULL,
    
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

-- Enable RLS on comments
ALTER TABLE comments ENABLE ROW LEVEL SECURITY;

-- STRICT POSTGRESQL RLS POLICY FOR SELECTION:
-- Comment content can ONLY be select-queried if:
-- 1. The selector is the commenter themselves (author of comment).
-- 2. The selector is the post author (recipient of backstage interaction).
-- 3. The comment has been spotlighted/featured with consent (shows as anonymous Q&A snippet to everyone).
CREATE POLICY comments_select_policy ON comments
    FOR SELECT USING (
        auth.uid() = commenter_id
        OR auth.uid() = (SELECT author_id FROM posts WHERE id = post_id)
        OR (is_featured = TRUE AND citizen_consent_given = TRUE)
    );

-- Insert: Only the commenter can insert their comment. Post author cannot insert comment as commenter.
CREATE POLICY comments_insert_policy ON comments
    FOR INSERT WITH CHECK (auth.uid() = commenter_id);

-- Update/Delete: Commenter can edit/delete their comment. Post author can delete or flag comments (due to absolute ownership).
CREATE POLICY comments_update_delete_policy ON comments
    FOR ALL USING (
        auth.uid() = commenter_id 
        OR auth.uid() = (SELECT author_id FROM posts WHERE id = post_id)
    );

--------------------------------------------------------------------------------
-- 5. CONTENT MODERATION, BLOCKS, AND REPORTS (Rule 4: Admin reporting link)
--------------------------------------------------------------------------------
CREATE TABLE moderations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    target_user_id UUID REFERENCES users(id) ON DELETE CASCADE NOT NULL,
    post_id UUID REFERENCES posts(id) ON DELETE SET NULL,
    comment_id UUID REFERENCES comments(id) ON DELETE SET NULL,
    reason TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW() NOT NULL
);

ALTER TABLE moderations ENABLE ROW LEVEL SECURITY;

-- Users can submit reports, admins can view them
CREATE POLICY moderations_insert_policy ON moderations
    FOR INSERT WITH CHECK (auth.uid() = reporter_id);

CREATE POLICY moderations_select_admin_policy ON moderations
    FOR SELECT USING (
        -- Assuming an 'is_admin' claim or role
        (auth.jwt() ->> 'role' = 'service_role')
        OR auth.uid() = reporter_id
    );

--------------------------------------------------------------------------------
-- 6. SECURITY DEFINER VIEWS FOR COUNT RETRIEVAL
-- Ensures counts can be safely queried using indexed aggregations 
-- without exposing rows bypassed by the RLS filter.
--------------------------------------------------------------------------------

-- Public-friendly count provider for a post's comments (Rule 1: "The Numbers Exist")
CREATE OR REPLACE FUNCTION get_post_comments_count(target_post_id UUID)
RETURNS BIGINT
LANGUAGE plpgsql
SECURITY DEFINER -- Runs with definition rights (circumventing RLS to return only aggregations)
AS $$
DECLARE
    total_count BIGINT;
BEGIN
    SELECT COUNT(*) INTO total_count FROM comments WHERE post_id = target_post_id;
    RETURN total_count;
END;
$$;

--------------------------------------------------------------------------------
-- 7. SQL TRIGGERS FOR CACHING STATISTICS (HIGH PERFORMANCE COUNTERS)
--------------------------------------------------------------------------------

-- Trigger Function to update post comments count cache
CREATE OR REPLACE FUNCTION cache_comments_counter_on_posts()
RETURNS TRIGGER AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        UPDATE posts SET comments_count = comments_count + 1 WHERE id = NEW.post_id;
    ELSIF TG_OP = 'DELETE' THEN
        UPDATE posts SET comments_count = GREATEST(0, comments_count - 1) WHERE id = OLD.post_id;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

CREATE TRIGGER trigger_cache_comment_count
AFTER INSERT OR DELETE ON comments
FOR EACH ROW EXECUTE FUNCTION cache_comments_counter_on_posts();
