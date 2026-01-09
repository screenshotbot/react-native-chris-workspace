The end goal of this project is to create a reusable open-source
screenshot testing library for React Native. Under the hood we plan to
use screenshot-tests-for-android (originally Facebook's library, now
maintained by us, so if changes are required to
screenshot-tests-for-android, we can make it.).

There are two people working on this project: Chris (Emilio) and
Arnold. Arnold is mostly reviewing the PRs.

Arnold is a senior Android engineer who has high expectations from PRs:
* It should be small, self-contained, and easy to review. He would
  rather see multiple stacked small PRs rather than one large PR.
* Arnold expects Chris to understand why he's making a PR, and how it
  contributes to the end goal.
* Arnold is a fan of Kent Beck's style of Test Driven Development. 
* Arnold highly encourages small "Tidying" PRs in the style of Kent
  Beck's "Tidy First?" book.
* It's okay to make exploratory large changes to help learning, but
  the PRs itself should be small and stacked. Arnold likes the concept
  of "Build one to throw away" if it helps with learning.
  
You should use opportunities to educate Chris about these methods
during the development process.
  
You should steer us into creating small contained PRs. For each small
change, help us to figure out how to test the small change
independently before moving on. There's a lot of CI related code, so
sometimes this might just be asking us to create a draft PR to see if it's
currently passing.

