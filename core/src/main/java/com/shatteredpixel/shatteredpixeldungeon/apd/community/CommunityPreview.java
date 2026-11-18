/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */


package com.shatteredpixel.shatteredpixeldungeon.apd.community;

// Informational local preview only. No account, queue, transport, or gameplay access.
public final class CommunityPreview {
    private CommunityPreview() {}
    public static final String COMMENTS = "Every submission will have its own threaded comment section. Players can comment, reply to other comments, suggest improvements and ask the original author questions. The author can reply. Discussions continue during voting. Comments are not votes; comment activity never increases the voting score.";
    public static final String COMMENT_PLACEHOLDER = "Comment threads will appear here under each submission. No submissions or comments are loaded in this offline preview. Posting and replying are planned for a future update.";
    public enum Page {
        SUBMISSIONS("Community submissions", "Month 1: propose ideas and discuss them",
                "Month 1 is for your own ideas and discussion. Players can read submissions, ask authors questions and suggest improvements. Authors can join the conversation. Ambitious ideas are welcome; difficulty or development time alone does not disqualify them."),
        QUALIFICATION("Qualification voting", "Month 2: vote to select the top 16",
                "Month 2 lets players upvote the submissions they want, as many as they like, but never their own. The top 16 advance to Month 3, and anything tied with 16th place advances too. Discussion continues during voting. Comments are not votes and do not increase a submission's voting score. Android and Steam share one community."),
        TOURNAMENT("Tournament brackets", "Month 3: weekly head-to-head elimination",
                "Month 3 starts with the final 16 and runs four weeks. In week 1 everyone picks up to 8 of them and the top 8 stay in. Weeks 2 to 4 are one-on-one matchups with one vote per player each week, until one idea wins. A tie goes to the higher seed. Eventually different categories and submission groups can progress through overlapping cycles."),
        WINNERS("Community selected content", "Follow selected ideas into development",
                "Winning ideas enter development. The approximate two-month implementation goal is not a deadline for discarding a winner."),
        REVIEWS("Run reviews and challenges", "Peer-review runs and attempt disputed situations",
                "Players will submit dungeon runs for peer review and discuss good or bad decisions. A reviewer calling a decision bad must attempt the same situation themselves. The required replay scope is undecided. Reviews are by players, not AI-generated judgments."),
        MODS("Mods and competitions", "Future compatible mods and character competitions",
                "Downloadable community mods with mobile compatibility are planned. A monthly modded-character competition and community voting can select characters for integration into APD. Download and competition rules are not finalized."),
        EVENTS("Community events", "Future shared community activities",
                "Community events are planned for the shared Android and Steam community. Players will eventually be able to participate in community activities. Event formats and participation rules remain undecided."),
        DEVELOPMENT("Development and testing", "Follow and test winning content in progress",
                "Players will eventually follow development of winning submissions and test content while it is being built. No live progress feed or community testing service is connected in this preview.");
        public final String title;
        public final String summary;
        public final String explanation;
        Page(String title, String summary, String explanation) {
            this.title = title;
            this.summary = summary;
            this.explanation = explanation;
        }
        public String availability() {
            String planned;
            switch (this) {
                case SUBMISSIONS: planned = "submit ideas, read player submissions and join threaded discussions"; break;
                case QUALIFICATION: planned = "vote on player submissions and see which 16 qualify"; break;
                case TOURNAMENT: planned = "follow brackets and vote in weekly elimination rounds"; break;
                case WINNERS: planned = "explore community selected additions as they enter development"; break;
                case REVIEWS: planned = "submit runs, discuss decisions and attempt reviewed situations"; break;
                case MODS: planned = "browse compatible mods and participate in character competitions"; break;
                case EVENTS: planned = "explore and participate in community events"; break;
                default: planned = "follow winning ideas and test content during development"; break;
            }
            return "Available now: read this offline preview. Planned: interactive community features in future updates. No submission, vote, review or download is sent here.\n\nFuture actions: " + planned + ".";
        }
    }
}
