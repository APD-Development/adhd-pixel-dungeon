package com.shatteredpixel.shatteredpixeldungeon.apd.community;

import org.junit.Test;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import static org.junit.Assert.*;

public class CommunityPreviewTest {
    @Test public void everyDestinationHasUniqueReadableContentAndExplicitOfflineAvailability() {
        Set<String> titles = new HashSet<>();
        Set<String> summaries = new HashSet<>();
        assertEquals(8, CommunityPreview.Page.values().length);
        for (CommunityPreview.Page page : CommunityPreview.Page.values()) {
            assertTrue(titles.add(page.title));
            assertTrue("each menu row has a unique summary", summaries.add(page.summary));
            assertTrue(page.summary.length() > 25);
            assertTrue(page.explanation.length() > 80);
            assertTrue(page.availability().contains("Available now: read this offline preview"));
            assertTrue(page.availability().contains("No submission, vote, review or download is sent here"));
            String text = (page.summary + page.explanation + page.availability()).toLowerCase();
            assertFalse(text.contains("december"));
            assertFalse(text.contains("january"));
            assertFalse(text.contains("25 testers"));
            assertFalse(text.contains("clan"));
            assertFalse(page.title.toLowerCase().contains("clan"));
        }
    }
    @Test public void undecidedRulesAreNotPresentedAsFinalizedAndWinnersAreNotDiscarded() {
        assertTrue(CommunityPreview.Page.TOURNAMENT.explanation.contains("final 16"));
        assertTrue(CommunityPreview.Page.QUALIFICATION.explanation.contains("top 16"));
        assertTrue(CommunityPreview.Page.REVIEWS.explanation.contains("replay scope is undecided"));
        assertTrue(CommunityPreview.Page.REVIEWS.explanation.contains("not AI-generated judgments"));
        assertTrue(CommunityPreview.Page.WINNERS.explanation.contains("not a deadline for discarding a winner"));
    }
    @Test public void playerFacingPreviewDoesNotDiscloseInternalCreativeRestrictionsOrSecrets() {
        StringBuilder text = new StringBuilder(CommunityPreview.COMMENTS)
                .append(CommunityPreview.COMMENT_PLACEHOLDER);
        for (CommunityPreview.Page page : CommunityPreview.Page.values()) {
            text.append(page.title).append(page.summary).append(page.explanation).append(page.availability());
        }
        String visible = text.toString().toLowerCase(java.util.Locale.ROOT);
        for (String privateTopic : new String[]{"vendetta", "cleric lore", "creator-reserved",
                "hidden", "achievement", "remember when you needed help"}) {
            assertFalse("Internal topic exposed: " + privateTopic, visible.contains(privateTopic));
        }
    }
    @Test public void commentsAreThreadedAuthorParticipatingAndNeverVotes() {
        assertTrue(CommunityPreview.COMMENTS.contains("own threaded comment section"));
        assertTrue(CommunityPreview.COMMENTS.contains("author can reply"));
        assertTrue(CommunityPreview.COMMENTS.contains("continue during voting"));
        assertTrue(CommunityPreview.COMMENTS.contains("never increases the voting score"));
        assertTrue(CommunityPreview.COMMENT_PLACEHOLDER.contains("No submissions or comments are loaded"));
    }
    @Test public void availabilityExplainsEachPagesOwnFutureActions() {
        Set<String> availability = new HashSet<>();
        for (CommunityPreview.Page page : CommunityPreview.Page.values()) {
            assertTrue("each destination explains its own planned actions", availability.add(page.availability()));
            assertTrue(page.availability().contains("Future actions: "));
        }
        assertTrue(CommunityPreview.Page.SUBMISSIONS.availability().contains("threaded discussions"));
        assertTrue(CommunityPreview.Page.QUALIFICATION.availability().contains("16 qualify"));
        assertTrue(CommunityPreview.Page.TOURNAMENT.availability().contains("weekly elimination"));
        assertTrue(CommunityPreview.Page.REVIEWS.availability().contains("attempt reviewed situations"));
    }

    @Test public void navigationHasNoUnlockQueueOrNetworkDependency() throws Exception {
        String scene = Files.readString(Paths.get(System.getProperty("apd.game.src"), "scenes/CommunityScene.java"));
        assertTrue(scene.contains("CommunityPreview.Page.values()[index]"));
        assertTrue(scene.contains("page.title, page.summary"));
        assertTrue(scene.contains("CommunityScene.this.onBackPressed()"));
        assertTrue(scene.contains("switchNoFade(TitleScene.class)"));
        assertFalse(scene.contains("UimsdUnlock"));
        assertFalse(scene.contains("BackendClient"));
        assertFalse(scene.contains("SubmissionQueue"));
        assertFalse(scene.contains("java.net"));
    }
}
