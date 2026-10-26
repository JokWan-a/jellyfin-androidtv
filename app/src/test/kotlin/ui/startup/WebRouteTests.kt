package org.jellyfin.androidtv.ui.startup

import android.content.Intent
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class WebRouteTests : FunSpec({
	val itemId = "6a7f2b1c-3d4e-4f50-8a9b-0c1d2e3f4a5b"

	test("search intent opens the search page with an encoded query") {
		getWebRoute(Intent.ACTION_SEARCH, "star wars & co", null) shouldBe "/search?query=star+wars+%26+co"
		getWebRoute(Intent.ACTION_SEARCH, null, null) shouldBe "/search?query="
	}

	test("item id from a view intent or channel extra opens the details page") {
		getWebRoute(Intent.ACTION_VIEW, null, itemId) shouldBe "/details?id=6a7f2b1c3d4e4f508a9b0c1d2e3f4a5b"
		getWebRoute(null, null, "6a7f2b1c3d4e4f508a9b0c1d2e3f4a5b") shouldBe "/details?id=6a7f2b1c3d4e4f508a9b0c1d2e3f4a5b"
	}

	test("missing or invalid item id opens no route") {
		getWebRoute(Intent.ACTION_MAIN, null, null) shouldBe null
		getWebRoute(Intent.ACTION_VIEW, null, "content://evil/x\"); alert(1)//") shouldBe null
	}
})
