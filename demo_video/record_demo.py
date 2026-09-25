import asyncio
import os
import subprocess
import glob
from playwright.async_api import async_playwright

async def main():
    print("Launching Playwright...")
    video_dir = "/config/Desktop/BuildWithGemini/demo_video"
    async with async_playwright() as p:
        browser = await p.chromium.launch(
            headless=True,
            args=["--no-sandbox", "--disable-setuid-sandbox", "--disable-dev-shm-usage"]
        )
        context = await browser.new_context(
            viewport={"width": 1440, "height": 900},
            record_video_dir=video_dir,
            record_video_size={"width": 1440, "height": 900}
        )
        page = await context.new_page()

        # -------------------------------------------------------------
        # Scene 1: Interactive PharmSync 3-Panel Canvas Dashboard
        # -------------------------------------------------------------
        print("Scene 1: Visiting Canvas Dashboard (http://localhost:8000)...")
        await page.goto("http://localhost:8000")
        await page.wait_for_timeout(2500)

        print("Clicking Synchronize Regimen button...")
        sync_button = page.locator("button:has-text('Synchronize Regimen')")
        if await sync_button.count() > 0:
            await sync_button.first.click()
        await page.wait_for_timeout(4500)

        print("Inspecting Synchronization Wheel & Pharmacy Counter Card...")
        await page.evaluate("window.scrollTo({top: 350, behavior: 'smooth'})")
        await page.wait_for_timeout(3000)
        await page.evaluate("window.scrollTo({top: 0, behavior: 'smooth'})")
        await page.wait_for_timeout(1500)

        # -------------------------------------------------------------
        # Scene 2: Interactive Mobile App Simulator
        # -------------------------------------------------------------
        print("Scene 2: Visiting Mobile Simulator (http://localhost:8000/mobile)...")
        await page.goto("http://localhost:8000/mobile")
        await page.wait_for_timeout(2000)

        print("Simulating camera scan on mobile...")
        camera_btn = page.locator("button[title='Scan Pill Bottle']")
        if await camera_btn.count() > 0:
            await camera_btn.first.click()
        await page.wait_for_timeout(2500)

        print("Clicking Synchronize Regimen on mobile phone...")
        mobile_sync = page.locator("#sync-btn")
        if await mobile_sync.count() > 0:
            await mobile_sync.first.click()
        await page.wait_for_timeout(4000)

        print("Tapping a pill card to mark as taken...")
        first_pill = page.locator(".pill-item").first
        if await first_pill.count() > 0:
            await first_pill.click()
        await page.wait_for_timeout(2500)

        # -------------------------------------------------------------
        # Scene 3: Rich Prompt in ADK Agent App Playground
        # -------------------------------------------------------------
        print("Scene 3: Visiting ADK Agent Dev UI (http://localhost:8080/dev-ui/?app=app)...")
        try:
            await page.goto("http://localhost:8080/dev-ui/?app=app")
            await page.wait_for_timeout(3500)

            textarea = page.locator("textarea")
            if await textarea.count() > 0:
                print("Entering rich multi-tool query...")
                prompt_text = "Elena Rostova takes Metformin, Lisinopril, and Atorvastatin. Run PharmSync optimization, look up Rx #301948 in the database, and show the exact NCPDP Field 420-DK adjudication override."
                await textarea.fill(prompt_text)
                await page.wait_for_timeout(1500)
                await page.keyboard.press("Enter")
                print("Waiting for agent to stream thought process and tool execution...")
                await page.wait_for_timeout(10000)
        except Exception as e:
            print(f"ADK step note: {e}")

        print("Closing context and saving video...")
        await context.close()
        await browser.close()
        print("Playwright recording completed!")

if __name__ == "__main__":
    asyncio.run(main())
