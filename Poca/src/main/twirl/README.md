# Twirl - How to contribute

## How it works

We have the main `Twirl` template `main.scala.html` which takes a __title__ as `String` and a __content__ as `Html`. The content should usually be another template. This main template will be the one called from `Scala`, as it contains the `<head>`, all the scripts and navigation menu.

### Call from Scala

To call a page from `Scala`, please use the following:

```Scala
html.main("<My title>", html.my_page(arg1, arg2, ...))
```

And replace `<My title>` and `my_page` resp. by the title of the page and by the name of the `Twirl` template you want to display.

### New page

In a new twirl template:

- Only write the content of the page, no `<head>`, `<body>`, doctype nor nav.
- If you wish, and it is cleaner to do so, put everything between a div:
    ```html
    <div class>
        <!-- Your content -->
    </div>
    ``` 
- Precede it by the following line:
    ```html
    <script>bodyToClass("my_style")</script>
    ```
    Replacing `my_style` by the CSS style you wish to have.

### CSS

To add your own CSS style for your page, please edit `src/main/resources/stylesheets/main.css` and use the style `my_style` (rename it to the style you defined in `bodyToClass("my_style")`). 

### Change Navigation Menu

Edit `src/main/twirl/main.scala.html`. Beware as it is dynamically created with JavaScript.

## Example

### Scala

```Scala
html.main("Poca - Sign-in", html.signin())
```

### HTML

```html
@()

<!-- This Source Code Form is subject to the terms of the Mozilla Public
   - License, v. 2.0. If a copy of the MPL was not distributed with this
   - file, You can obtain one at https://mozilla.org/MPL/2.0/. -->

<script>bodyToClass("signin")</script>

<div class="signin">
    <h1>Sign in</h1>

    <p>Here you can sign in to our marvelous web application.</p>

    <form action=/api/do_login method="post">
        <label for="username">username :</label><br>
        <input type="text" name="username"></input><br>
        <label for="password"></label>password :<br>
        <input type="password" name="password"></input><br>
        <input type="submit">
    </form>
</div>
```

### CSS

```css
body.signin,
body.signup {
    background: url("https://cdn.pixabay.com/photo/2016/11/23/15/29/animal-1853533_960_720.jpg") no-repeat;
    background-size: cover;
}

.signin h1,
.signup h1 {
    color: black;
    text-align: right;
    margin: 5% 22% 10%;
}

.signin p,
.signup p {
    text-align: right;
    margin: 5% 13% 2%;
}

.signin form,
.signup form {
    text-align: center;
    margin: 10% 5% 20% 50%;

}

.signin input[type=submit],
.signup input[type=submit] {
    margin: 8px;
    border: 2px solid black;
    width: 35%;
    border-radius: 25px;
    background-color: black;
    color: #d1d1e0;
}

.signin input,
.signup input {
    padding: 12px 35%;
    margin: 8px;
    width: 30%;
    border: 2px solid black;
    border-radius: 25px;
    background-color: transparent;
    color: black;
}

.signin label,
.signup label {
    display: inline;
}
```

