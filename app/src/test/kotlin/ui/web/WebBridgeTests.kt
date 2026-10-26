package org.jellyfin.androidtv.ui.web

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.jellyfin.sdk.model.serializer.toUUID

class WebBridgeTests : FunSpec({
	test("parseWebCredentials() reads the access token from the evaluateJavascript result") {
		val result = "\"{\\\"Servers\\\":[{\\\"Id\\\":\\\"abc\\\",\\\"UserId\\\":\\\"def\\\",\\\"AccessToken\\\":\\\"token\\\"}]}\""
		parseWebCredentials(result) shouldBe "token"
	}

	test("parseWebCredentials() returns null when there are no credentials") {
		parseWebCredentials("null") shouldBe null
		parseWebCredentials("\"{\\\"Servers\\\":[]}\"") shouldBe null
		parseWebCredentials("\"{\\\"Servers\\\":[{\\\"Id\\\":\\\"abc\\\"}]}\"") shouldBe null
	}

	test("parseWebCredentials() returns null for malformed input") {
		parseWebCredentials("") shouldBe null
		parseWebCredentials("{\"Servers\":[]}") shouldBe null
		parseWebCredentials("\"not json\"") shouldBe null
	}

	test("parseLoadPlayerOptions() parses all fields and ignores unknown ones") {
		val options = parseLoadPlayerOptions(
			"""{"ids":["b2e8f9a1c3d44e5f8a7b6c5d4e3f2a1b","00000000-0000-0000-0000-000000000001"],""" +
				""""startIndex":1,"startPositionTicks":12345,"mediaSourceId":"x"}"""
		)

		options shouldBe LoadPlayerOptions(
			ids = listOf("b2e8f9a1c3d44e5f8a7b6c5d4e3f2a1b".toUUID(), "00000000-0000-0000-0000-000000000001".toUUID()),
			startIndex = 1,
			startPositionTicks = 12345,
		)
	}

	test("parseLoadPlayerOptions() allows null or missing start values") {
		val id = "b2e8f9a1c3d44e5f8a7b6c5d4e3f2a1b"
		val expected = LoadPlayerOptions(ids = listOf(id.toUUID()))

		parseLoadPlayerOptions("""{"ids":["$id"],"startIndex":null,"startPositionTicks":null}""") shouldBe expected
		parseLoadPlayerOptions("""{"ids":["$id"]}""") shouldBe expected
	}

	test("parseLoadPlayerOptions() returns null for malformed input") {
		parseLoadPlayerOptions("") shouldBe null
		parseLoadPlayerOptions("{}") shouldBe null
		parseLoadPlayerOptions("""{"ids":["not-a-uuid"]}""") shouldBe null
	}
})
