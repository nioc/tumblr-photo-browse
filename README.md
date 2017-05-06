# Tumblr Photo Browse
[![Build Status](https://travis-ci.org/nioc/tumblr-photo-browse.svg?branch=master)](https://travis-ci.org/nioc/tumblr-photo-browse)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/d6272ccb74534606937d980575c9a91c)](https://www.codacy.com/app/nioc/tumblr-photo-browse?utm_source=github.com&utm_medium=referral&utm_content=nioc/tumblr-photo-browse&utm_campaign=Badge_Grade)
[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](http://www.gnu.org/licenses/gpl-3.0)

Tumblr Photo Browse is an open source Android application for browsing photos posted on [Tumblr](https://www.tumblr.com).

## Goal
Providing an Android Tumblr application:
- photo focused,
- user-friendly,
- allow multi accounts,
- without annoying ads.

Some functions made with love:
- see recently updated blogs,
- browse photos posts,
- like a post,
- navigate to original blog,
- follow and unfollow blogs,
- get EXIF data (camera and lens model, focal length, aperture, shutter speed, ISO rating) from a photo.

![Connect account](/docs/connect_account.png)
![Browse blog posts](/docs/browse_blog_posts.png)
![Fullscreen display](/docs/fullscreen_display.png)
![Manage followed blogs](/docs/manage_followed_blogs.png)

## Installation and usage

#### 0. Common software dependencies
You will need [Android Studio](https://developer.android.com/studio/index.html) and a git tool (it should not be a problem as you're probably read this on GitHub).

#### 1. Fork or download this repository
Easiest way is to download [the repository](https://github.com/nioc/tumblr-photo-browse/archive/master.zip), but you can also clone/fork it with Git.

#### 2. Working with Android Studio
With Android Studio, you will have to :
- import the project.
- edit the `gradle.properties` file to set your own Tumblr API key `TUMBLR_API_CONSUMER_KEY` and secret `TUMBLR_API_CONSUMER_SECRET` ; to do this you will have to [register your application](https://www.tumblr.com/oauth/apps).
- build project and deploy APK to your device.

## Reporting issues
Please refer to the [GitHub issue tracker](https://github.com/nioc/tumblr-photo-browse/issues).

## Contributing
The project is open and any contribution is welcome!

A little how-to for GitHub:

1. [Fork it](https://help.github.com/articles/fork-a-repo/)
2. Create your feature branch: `git checkout -b my-new-feature`
3. Commit your changes (with a detailed message): `git commit -am 'Add an awesome feature'`
4. Push to the branch: `git push origin my-new-feature`
5. Submit a pull request

## Versioning
We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/nioc/tumblr-photo-browse/tags).

## Authors
* **[Nioc](https://github.com/nioc/)** - *Initial work*

See also the list of [contributors](https://github.com/nioc/tumblr-photo-browse/contributors) who participated in this project.

## License
This project is licensed under the GNU General Public License v3.0 - see the [LICENSE](LICENSE.md) file for details

## Included libraries

This project uses the following:
- [Jumblr](https://github.com/tumblr/jumblr) (Apache License 2.0)
- [Volley](https://android.googlesource.com/platform/frameworks/volley.git) (Apache License 2.0)
- [PhotoView](https://github.com/chrisbanes/PhotoView) (Apache License 2.0)
- [Picasso](https://github.com/square/picasso) (Apache License 2.0)
- [ScribeJava](https://github.com/scribejava/scribejava) (MIT)
- [Gson](https://github.com/google/gson/) (Apache License 2.0)
- [Greedo Layout for Android](https://github.com/500px/greedo-layout-for-android) (MIT)
- [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) (Apache License 2.0)
