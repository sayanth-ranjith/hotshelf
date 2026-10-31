# hotshelf docs

These docs are the "textbook" for the project. The code shows *what* hotshelf does; these docs explain *why* it does it that way.

## How to read them

1. **[roadmap.md](roadmap.md)**: the whole journey on one page. Start here.
2. **[design-principles.md](design-principles.md)**: the SOLID principles and design patterns we use, each tied to a concrete
   hotshelf class. Come back to it whenever an iteration says "Strategy" or "DIP".
3. **[iterations/](iterations/)**: one file per step. Each file has:
   - **Goal**: what works when the iteration is done.
   - **Concepts you'll learn**: the caching and Java ideas it introduces.
   - **Scope**: what is in and what is deliberately out.
   - **Design**: the classes, the patterns used, *why* they were chosen, and the alternatives rejected.
   - **Definition of done**: the tests that must pass.
   - **Pitfalls**: the bugs people usually write here.
   - **Learning log**: filled in *after* the iteration with what surprised us, benchmark numbers, and open questions.
4. **[decisions.md](decisions.md)**: a short log of architecture decisions (ADRs), so we remember why something is the way it is.

## The working loop

```
read iteration doc → discuss/adjust design → branch iteration/NN-name
   → write tests + code → PR (description repeats the "why") → review & merge
   → fill in the learning log → tick the box in README.md
```

Detailed iteration files exist for **v1** only. Later versions are outlined in the roadmap and get their detailed files
when we reach them. By then we will know much more, and the plan should reflect that.
