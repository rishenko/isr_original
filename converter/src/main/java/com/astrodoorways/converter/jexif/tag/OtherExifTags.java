package com.astrodoorways.converter.jexif.tag;

import be.pw.jexif.enums.DataType;
import be.pw.jexif.enums.tag.Tag;

public enum OtherExifTags implements Tag {

	MAKE("Make", false, false, false, DataType.STRING), MODEL("Model", false, false, false, DataType.STRING), FILTER(
			"Filter", false, false, false, DataType.STRING), LENS("Lens", false, false, false, DataType.STRING);
	;
	private final boolean avoided;
	private final boolean unsafe;
	private final boolean protectedField;
	private final String name;
	private final DataType type;

	private OtherExifTags(final String name, final boolean unsafe, final boolean avoided, final boolean protectedField,
			final DataType type) {
		this.avoided = avoided;
		this.unsafe = unsafe;
		this.protectedField = protectedField;
		this.name = name;
		this.type = type;
	}

	@Override
	public boolean isAvoided() {
		return avoided;
	}

	@Override
	public boolean isUnsafe() {
		return unsafe;
	}

	@Override
	public boolean isProtectedField() {
		return protectedField;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public DataType getType() {
		return type;
	}
}