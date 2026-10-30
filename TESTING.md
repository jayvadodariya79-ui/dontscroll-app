# Morning test checklist (Samsung)

## Install
1. Install the APK (allow "Install unknown apps" for your browser/files app if asked).
2. Open SwipeStop → Get started → set a **small** reel budget for testing, e.g. **10**.
3. Allow Accessibility: Agree → Installed apps → SwipeStop → On → Allow.
   - If it says **Restricted setting**: App info → ⋮ (top right) → **Allow restricted settings** → try again.
4. Allow notifications. App info → Battery → **Unrestricted**.

## Tests
| # | Do this | Expect |
| --- | --- | --- |
| 1 | Open Instagram → Reels tab | Counter bubble at the top: `SwipeStop 1/10` |
| 2 | Swipe 5 reels | Counter goes up by 1 per swipe (not 2, not 0) |
| 3 | Swipe back up one reel | Counter does **not** go up |
| 4 | Open comments on a reel, close them | No extra count |
| 5 | Go to the Instagram home feed and scroll | Bubble disappears, no counting |
| 6 | Open YouTube → Shorts, swipe 3 | Counts go up; Insights shows YouTube |
| 7 | Keep going to 8 reels | Violet "Heads up" banner |
| 8 | Reach 10 | Full "Time to stop" screen; **Leave now** takes you out of Reels |
| 9 | Open Reels again | Stop screen again; after 10 s "5 more minutes" works |
| 10 | SwipeStop → Insights | Hourly bars, Total Reels, Time spent and By app match what you did |
| 11 | Goals tab | Today's goal shows reels used; pick a challenge |
| 12 | Settings → Limits → "Block Instagram Reels completely" | Reels shows the stop screen at once; DMs/feed still work |
| 13 | Snapchat Spotlight / Facebook Reels (beta) | Note whether it counts |
| 14 | Add the home-screen widget | Shows today's reels and time |

## If something is off
Settings → **Detection check** → reproduce it → **Copy log** → paste the log to Claude. It shows what the tracker saw (view IDs, decisions), never screen text.
