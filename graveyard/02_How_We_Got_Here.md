# How We Got Here

> In your journey to this book, you might have already learned one way to think about programming, and possibly several.

You might have succeeded creating a project using that approach.

You might also have encountered programming constructs inspired by functional programming.

For example, many languages have introduced lambdas along with support for streams and functional operations like `map`.

Some languages allow functions to be 
- be created anonymously
- stored in variables
- passed as parameters to other functions
- returned from other functions

C++, especially more recent versions, has added a number of features that support functional-style programming.

If, however, you are coming from an imperative programming background, these functional-style devices can seem arbitrarily complicated.
Why go through the trouble of an effect-oriented API when plain `var`s, `for` loops, and `:Unit` exist?
Sometimes it seems like functional programmers write code like this just to be fancy.

To understand what's really behind this different way of thinking about programming, we will start with some history.

## A Different Goal

In the early days of programming, 
    most code was written in an assembly language for a particular machine.

Assembly language had primitive function-like constructs called subroutines,
    but they were so much work to set up and safely use that programmers often just wrote the code inline.

Even if you wanted to create reusable code, 
    it was often easier to just "goto" a piece of code and use global variables,
    rather than bothering with passing arguments and returning results.

In those early days, 
    a "high level language" meant a language like C that passed arguments and returned results for you.
This suddenly made writing functions much easier, safer, and faster.

But the habits of assembly-language programmers didn't vanish overnight, 
    and people were still prone to writing obtuse monolithic programs 
       ---often a single function for the whole program---
    and jumping around in their code using `goto`s.

Many programs were written and maintained by a small number of programmers, 
    or even a single programmer, 
    for whom that code made sense.
Anyone else reading the code would be baffled.

In addition, the idea of calling code written by other people was fairly foreign.
If you didn't write the code yourself, how would you know if it does what you want? 
Documentation and testing were also primitive, if they existed at all.
Thus "code reuse" was a big goal.
There were many attempts within companies to create cultures of code reuse,
    because many programmers were rewriting the same functionality from scratch, 
    over and over within the same organization.

Monolithic programs that didn't reuse code were also a maintenance nightmare.
It was not uncommon for such programs to be thrown away and rewritten just to add some new features.
Not surprisingly, 
    writing everything from scratch also took a lot longer than reusing common functionality. 
The new question became:
    "how do we make code reuse easier?"

Languages like C and Pascal made it easy to both write and call functions.
They were a big improvement over assembly language, 
However, the monolithic habits of assembly-language programmers persisted into those new languages.
Libraries grew bigger and more complex,
    and using those libraries was not easy.
In C, 
    for example, 
    you would often have to allocate memory before calling a function, 
    and then remember to release the memory after that function returned.

You also had to learn how to pass information from one library function to another.
You had to learn how each library reported errors, 
    which typically varied in strategy from one library to the next.
Code could be reused, 
    but it wasn't easy.

At this point, object-oriented programming brought several good ideas to the world.

It allowed you to package data structures with automatic initialization and cleanup,
    with all the functions that act upon that data.

If you wanted to reuse some code,
    you created an object with some initialization values, 
    then sent messages to that object to produce the desired results.
This *did* make code reuse easier,
    and helped speed up program creation.
It also came with the distraction of inheritance polymorphism.
This birthed an entire education and consulting industry explaining how to cram every design into an inheritance hierarchy 
Inheritance polymorphism does sometimes prove useful, 
        but not everywhere, 
        all the time.

C++ added object-oriented features from the Simula language while maintaining backward compatibility with the C language.
C++ had a strong emphasis on static type checking.
Java was created as a counterpoint to C++ and was heavily inspired by the Smalltalk language.

Smalltalk's success came from its ability to rapidly create systems by adding functionality to existing objects.
This introduced a conundrum, because Smalltalk is a dynamic language, and Java, like C++, is statically typed.
Smalltalk can be thought of as supporting an experimental style of programming: you send a message to an object and discover at runtime whether the object knows what to do with that message.
But C++ and Java ensure everything is valid, at compile time 
    (though there are escape mechanisms that bypass that type checking).

The Agile methodologies that began in the early 2000's were another attempt to produce software faster,
    but through a more bottom-up lens.
Agile was primarily focused on improving communication between stakeholders and developers,
    and producing more rapid round trips between needs and experiments.
This improves the chance that the stakeholders will get what they need, faster.
Agile helped the process of software development, 
    but again,
    the focus is on developing software quickly,
    not on developing reliable software.

This language history highlights that the fundamental goal of the various techniques was speed of creation.
There seems to be an underlying assumption that these approaches will somehow automatically create more reliable software.
As a result,
    we have languages that quickly create unreliable software.
And in many cases we've been able to get by with that.
For one thing,
    this approach has greatly advanced testing technology,
    because it was necessary.
Customers have learned to put up with buggy software.
They've often been willing to accept buggy software when the alternative is no software at all.

The world has changed.
Back then, 
    the drive was to speed up activities that humans were doing.
Those humans could compensate for bugs.
Now, 
    however, 
    more and more software is doing things that humans can't do,
    so failures in software cannot be propped up by humans.
Unreliable software is no longer an inconvenience,
    but a serious problem.

Quickly creating unreliable software is no longer acceptable.
We must delve into the reasons that software fails
    ---either it doesn't do what it's supposed to,
        or it just breaks.

## Reuse

How do we create software? 
When you first learned to program,
    you probably solved problems by writing code using the basic constructs of your language.
But at some point you began realizing that you could only produce and debug so much code by yourself.
If you could use code that was already written and debugged by other people,
    you could produce solutions faster.

You might have gone through a cut-and-paste phase before discovering that formalized libraries were easier and more reliable.
Even then, 
    library ease of use depended on the sophistication of your language.
For the reasons mentioned,
    using a C library could be tricky and difficult.
C++ made this much easier and paved the way for the acceptance of languages like Java, Python, Scala, and Kotlin.
Indeed, 
    any new language that doesn't support easy code reuse is not taken seriously.

But code reuse in object-oriented languages was still limited.
You could either use objects in a library directly,
    or you could add those library classes into new classes using *composition*.
This was a big step, 
    and it helped a lot.
In contrast,
    composing C libraries wasn't particularly realistic
        ---it was just too messy and complicated.

The problem is reliability.
If you create a new class using composition,
    you combine problems with the existing class(es) with any bugs you introduced in your new class.
As you build up bigger systems,
    the problem of bugs multiplies.

To compose systems rapidly *and* reliably,
    we return to first principles and figure out how to:

1. Create basic components that are completely reliable.
2. Combine those components in a way that does not introduce new bugs.

To achieve these goals we must examine the fundamentals of how we think about software.

## Pure Functions

Composition in an object-oriented language doesn't attempt to manage bugs,
    so it ends up amplifying them.
If we want to compose pieces of software,
    we must discover what creates a fundamentally unbreakable piece,
    then how to assemble those pieces without producing a broken result.

First,
    what constitutes a reliable,unbreakable piece of software? 
We've already seen that objects are not inherently unbreakable,
    so we'll move back to a more basic software component: 
        the function.
What are the characteristics of an unbreakable function?

What we want is the same kind of function we have in math.
This means that the function does *nothing* except produce a result from its arguments.
And given the same arguments,
    it always produces the same result.

This behavior imposes additional constraints: 
    The function cannot affect its environment, 
        and the environment cannot affect the function
            ---otherwise, 
                the function has a *history* and behaves differently at one point in time vs. another.
Running that function doesn't necessarily produce the same results from one call to the next.

If a function affects its environment,
    we call that a *side effect*.
It's "on the side" because it's something other than just producing a result from the function.
Many programming languages have side effects built in,
    in the form of *statements*.
A statement doesn't return a result,
    so the only reason to execute a statement is for its side effect.
For example,
    "print" is typically a statement that returns nothing but causes the side effect of displaying text on a console.
On the other hand, 
    an *expression* produces 
        ("expresses") 
        a result.
A functional language avoids statements and attempts to make everything an expression that produces a result.

What about the environment affecting the function?
If our program behaves differently at different times,
    that means the environment's time is affecting the function.
More subtly,
    we should consider concurrency.
If multiple tasks are running in our program,
    then at any point another task might see variables in our function.
A variable can change,
    so that means this other task might see different values at different points in the function's execution.
And if that variable is modified by some other task,
    we have no way of predicting the result,
    and we don't get the reliable mathematical function that we want.

We solve this problem through *immutability*.
That is,
    instead of using variables,
    we create values that *cannot change*.
This way,
    it doesn't matter if an external task sees our values,
    because it will only see that one value and not something that is different from one moment to the next.
And the external task cannot change the value and cause the function to produce a different result.

Functions that behave mathematically,
    that always produce the same results from the same inputs and have no side effects,
    are called *pure functions*.
When we add the additional constraint of immutability,
    we produce functions that compose without introducing points of breakage.
We can rely on such functions.
Note - an operator like `.retry` makes no sense for a pure operation.

## Core Differences Between OO and Functional

An OO language worries about managing state.
It "encapsulates" a data structure in privacy and surrounds it with custom methods (aka member functions) which are ideally the only way to access and modify the state of that data structure.
This is important because an OO data structure is typically mutable.
This OO ceremony attempts to create predictability by knowing how the data structure can be mutated.

Functional programming abstracts common behavior into reusable functional components. 
These components are adapted to specific needs using other functions. 
This is why lambdas are so important, because you constantly need to adapt general code to specific purposes, 
They enable concise code that would otherwise be awkward and intrusive to right as a standalone function.

Functions in a functional language don't need to be tied to a particular data structure.
Thus, they can often be written for more general use and to reduce duplication.
Functional languages come with a general set of well-tested, reusable operations that can be applied in many situations.

A functional language relies on immutability.
An immutable data structure doesn't need privacy because it is safe for any task to read, and it cannot be written (only initialized).
Ojects in functional languages are simply naked data structures along with constructors.
When everything is immutable, there is no need for private properties or methods to maintain the state of an object.

## Summary: Style vs Substance

Functional programming abstracts common behavior into reusable functional components.
These components are adapted to specific needs using other functions. 
This is why lambdas are so important, because you constantly need to adapt general code to specific purposes, often with a brief amount of code that would otherwise be awkward and intrusive to write as a standalone function.

The two things we do with functions is compose them to make more complex functions, and adapt to them to our specific problem.

However, there could be significantly more than:

- a function's ability to create other functions
- transforming elements in a collection using `map`

Those are indeed important benefits, but they just dip into the possibilities.
Adopting some of the styles found in functional programming does not make a language functional.

In this book we want to get to the heart of what it means to be functional.
In particular, we want to show what it takes to make *reliable* functional code that can be composed without propagating or amplifying flaws in its components.


## Were The Missiles Launched?

08:07 AM January 13, 2018

Televisions, Radios, and Cell Phones across Hawaii suddenly flash an alert:

C> "BALLISTIC MISSILE INBOUND THREAT TO HAWAII. SEEK IMMEDIATE SHELTER. THIS IS NOT A DRILL"

Local communities sound alarms.

Calls to 911 jam the phone lines.

Panicked internet searches overwhelm data networks.

Students sprint from classrooms to fallout shelters.

Parents say goodbye to their children.

## Untangling the Chaos

Thankfully, no missiles were launched that day.

During what should have been a quiet system test, an employee at the Hawaii Emergency Management Agency accidentally pushed the wrong button.
From the [Washington Post](https://www.washingtonpost.com/news/post-nation/wp/2018/01/14/hawaii-missile-alert-how-one-employee-pushed-the-wrong-button-and-caused-a-wave-of-panic/):

C> "He clicked the button to send out an actual notification on Hawaii's emergency alert interface during what was intended to be a test of the state's ballistic missile preparations computer program."
C> The employee was prompted to choose between the options "test missile alert" and "missile alert", had selected the latter, initiating the alert sent out across the state.

Here is the system's control screen:

![](../Chapters/images/HawaiiAlertSystem.jpg)

This cluster of inconsistently named links increased the likelihood of mistakes.
Basic changes would drastically simplify proper use of the alerts.
Imagine the earlier mishaps that moved "False Alarm" to the top of the list.

We believe the system was doomed long before the interface was created.

The effects of this system were:

- Sending messages to Cell Phones
- Playing warnings on Radio frequencies
- Displaying banners on Television stations

## The State of Software

There are many other examples of carefully-built software systems failing disastrously:

- The Ariane 5 rocket self-destructed on 4 June 1996 because of a malfunction
  in the control software (the program tried to stuff a 64-bit number into a
  16-bit space).

- The American Northeast Power Blackout, August 14 2003.

- The NASA Mars Climate Orbiter, September 23, 1999. The orbiter was programmed
  for metric but ground control software used non-metric English.

The list goes on; just search for something like "Famous Software Failures" to see more.
And consider security - all the applications you use that are constantly being updated with security patches.
What about those that aren't?
Are they that good, or is security being ignored?

How did things get so bad?

## The Software Crisis

In the 70's and 80's, the idea of the *Software Crisis* emerged.
This can be summarized as: "We can't create software fast enough."
One of the most popular attempts to solve this problem was *Structured Analysis & Design*, which was a way to understand a problem and design a solution using existing imperative languages.

The real problem that Structured Analysis & Design set out to solve was big monolithic pieces of code.
When one programmer was able to solve the entire problem, the structure of the program didn't matter as much.
But as software needs grew, this approach didn't scale.
In particular, you couldn't finish a project more quickly by adding more programmers, because there wasn't a way to hand off portions of a program to multiple programmers.
To do that, teams needed some way to break down the complexity of the program into individual functions---functions that might someday be reused.
This was seen as the reason for the Software Crisis.

Structured Analysis was an attempt to discover the individual functions in a program.
But it was a top-down approach, and it assumed these functions could be determined before any code is written.
Structured Analysis & Design continued the approach of "big up-front design."
The analyst produced the structure, and then the programmers implemented it.

Experienced programmers know that a design that cannot evolve during development is doomed to failure: both programmers and stakeholders learn things during development.
You discover much of your structure *as* you're building the program, and not on a whiteboard.
Building a program reveals things you didn't know were important when you designed the solution.

From this book's perspective, the most fundamental problem with Structured Analysis & Design was that it only paid lip service to the idea of reliability.
There was nothing about reliability truly integrated into Structured Analysis & Design.

Structured Analysis & Design was motivated by a business problem: "how do we create software faster?"
Virtually every language that came out in its aftermath focused on development speed.
Not reliability.
So we produced a lot of languages to quickly create unreliable software.
