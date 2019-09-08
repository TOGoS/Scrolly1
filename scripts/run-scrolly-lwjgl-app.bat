@echo off
: This was the path to LWJGL on Dellie.
: Obvs it must be adjusted if you have LWJGL in a different place.
set lwjgl_path=C:\Apps\lwjgl-2.9.3
java -Djava.library.path=%lwjgl_path%\native\windows -cp %lwjgl_path%\jar\lwjgl.jar;target togos.scrolly1.lwjgl.LWJGLScrollyCanvas