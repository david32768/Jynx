Jynx ignores all lines in a file before the first directive
(a line which after trimming starts with '.' ),
so if a precomment starts with '.' use the following gadget (for Java statements) 
```
	Stream.of(anEnum.values())
		.forEach(System.out::println);
```
```
	Stream.of(anEnum.values())
/**/		.forEach(System.out::println);
```
