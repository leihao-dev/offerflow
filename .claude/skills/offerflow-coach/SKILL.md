---
name: offerflow-coach
description: Guide logging job applications and interview debriefs in OfferFlow. Use after submitting a job application or completing an interview.
when_to_use: "投了简历, 面试完了, 跟进, 复盘, 记录投递, log application, debrief interview"
---

# OfferFlow Coach

Prefix your first line with 📋 when actively coaching a logging session.

Help the user keep OfferFlow up to date. OfferFlow runs at http://localhost:8080 (start with `gradlew.bat bootRun`).

## After a new job application

Collect these fields before suggesting anything else:

1. Company name
2. Position title
3. Source (Boss / 内推 / 官网 / 猎头等)
4. Current stage (default: APPLIED)
5. Next follow-up date

Then either:
- Direct them to http://localhost:8080/applications/new, or
- Summarize the entry so they can paste into the form.

## After an interview (within 24 hours)

Collect:

1. Interview date and round (一面 / HR / 终面)
2. Questions asked
3. Self assessment — what went well and poorly
4. Improvements for next time

Direct them to the application detail page → **+ 新增复盘**.

## Weekly (CC assignment)

Suggest 3 bullets for `JOURNAL.md`:
- One thing that worked with CC
- One friction point (be honest)
- One "原来还能这样" moment

## Do not

- Write code unless the user asks to change OfferFlow itself
- Skip follow-up date — it's the core value of the pipeline view
