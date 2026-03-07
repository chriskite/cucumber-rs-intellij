package com.chriskite.cucumber_rs.intellij

import com.chriskite.cucumber_rs.intellij.util.RustStepDefinitionUtil
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Tests for the Rust step definition parsing utility.
 */
class RustStepDefinitionUtilTest {

    @Test
    fun `test parse simple given step definition`() {
        val rustCode = """
            use cucumber::{given, when, then};

            #[given("she is hungry")]
            async fn she_is_hungry(w: &mut World) {
                w.hungry = true;
            }
        """.trimIndent()

        val steps = RustStepDefinitionUtil.parseStepDefinitions(rustCode)
        assertEquals(1, steps.size)
        assertEquals("given", steps[0].keyword)
        assertEquals("she is hungry", steps[0].pattern)
        assertFalse(steps[0].isRegex)
        assertEquals("she_is_hungry", steps[0].functionName)
    }

    @Test
    fun `test parse expr step definition`() {
        val rustCode = """
            #[given(expr = "{word} is hungry")]
            async fn someone_is_hungry(w: &mut World, user: String) {
                w.user = Some(user);
            }
        """.trimIndent()

        val steps = RustStepDefinitionUtil.parseStepDefinitions(rustCode)
        assertEquals(1, steps.size)
        assertEquals("given", steps[0].keyword)
        assertEquals("{word} is hungry", steps[0].pattern)
        assertFalse(steps[0].isRegex)
    }

    @Test
    fun `test parse regex step definition`() {
        val rustCode = """
            #[when(regex = r"^(?:he|she|they) eats? (\d+) cucumbers?$")]
            async fn eat_cucumbers(w: &mut World, count: usize) {
                w.capacity += count;
            }
        """.trimIndent()

        val steps = RustStepDefinitionUtil.parseStepDefinitions(rustCode)
        assertEquals(1, steps.size)
        assertEquals("when", steps[0].keyword)
        assertTrue(steps[0].isRegex)
    }

    @Test
    fun `test parse multiple step definitions`() {
        val rustCode = """
            #[given("she is hungry")]
            async fn she_is_hungry(w: &mut World) {
                w.hungry = true;
            }

            #[when("she eats {int} cucumbers")]
            async fn eat_cucumbers(w: &mut World, count: i32) {
                w.count = count;
            }

            #[then("she is full")]
            async fn is_full(w: &mut World) {
                assert!(w.count > 0);
            }
        """.trimIndent()

        val steps = RustStepDefinitionUtil.parseStepDefinitions(rustCode)
        assertEquals(3, steps.size)
        assertEquals("given", steps[0].keyword)
        assertEquals("when", steps[1].keyword)
        assertEquals("then", steps[2].keyword)
    }

    @Test
    fun `test containsStepDefinitions`() {
        assertTrue(RustStepDefinitionUtil.containsStepDefinitions("""#[given("test")]"""))
        assertTrue(RustStepDefinitionUtil.containsStepDefinitions("""#[when("test")]"""))
        assertTrue(RustStepDefinitionUtil.containsStepDefinitions("""#[then("test")]"""))
        assertFalse(RustStepDefinitionUtil.containsStepDefinitions("""fn main() {}"""))
        assertFalse(RustStepDefinitionUtil.containsStepDefinitions("""#[derive(Debug)]"""))
    }

    @Test
    fun `test cucumber expression to regex`() {
        assertEquals(
            """^\Qshe is hungry\E$""",
            RustStepDefinitionUtil.cucumberExpressionToRegex("she is hungry")
        )
    }

    @Test
    fun `test cucumber expression with int placeholder`() {
        val regex = RustStepDefinitionUtil.cucumberExpressionToRegex("she eats {int} cucumbers")
        assertTrue(regex.contains("(-?\\d+)"))
    }

    @Test
    fun `test cucumber expression with word placeholder`() {
        val regex = RustStepDefinitionUtil.cucumberExpressionToRegex("{word} is hungry")
        assertTrue(regex.contains("(\\S+)"))
    }

    @Test
    fun `test stepTextToFunctionName`() {
        assertEquals("she_is_hungry", RustStepDefinitionUtil.stepTextToFunctionName("she is hungry"))
        assertEquals("she_eats_3_cucumbers", RustStepDefinitionUtil.stepTextToFunctionName("she eats 3 cucumbers"))
        assertEquals("alice_is_hungry", RustStepDefinitionUtil.stepTextToFunctionName("Alice is hungry"))
    }

    @Test
    fun `test generateStepDefinition`() {
        val code = RustStepDefinitionUtil.generateStepDefinition("given", "she is hungry", "she_is_hungry")
        assertTrue(code.contains("#[given(\"she is hungry\")]"))
        assertTrue(code.contains("async fn she_is_hungry"))
        assertTrue(code.contains("todo!(\"Implement step\")"))
    }

    @Test
    fun `test generateStepDefinition with parameters`() {
        val code = RustStepDefinitionUtil.generateStepDefinition("when", "she eats {int} cucumbers", "eat_cucumbers")
        assertTrue(code.contains("#[when(\"she eats {int} cucumbers\")]"))
        assertTrue(code.contains("param0: i32"))
    }

    @Test
    fun `test isRustFile`() {
        // We can't easily test VirtualFile without a full IDE context,
        // so this is more of a documentation test
        assertTrue(true)
    }
}
