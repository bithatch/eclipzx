package uk.co.bithatch.eclipzpp;

public record DefineDef(String name, String value, String... parameters) {

	public static DefineDef of(String nameParametersAndValue) {
		var idx = nameParametersAndValue.indexOf('=');
		return idx == -1 ? of(nameParametersAndValue, "") :
				of(nameParametersAndValue.substring(0,idx).trim(),
					nameParametersAndValue.substring(idx + 1).trim());
	}

	public static DefineDef of(String nameAndParameters, String val) {
		var idx = nameAndParameters.indexOf('(');
		if(idx == -1) {
			return new DefineDef(nameAndParameters, val);
		}
		else {
			var eidx = nameAndParameters.lastIndexOf(')');
			return new DefineDef(
				nameAndParameters.substring(0, idx),
				val,
				eidx == -1 ? new String[0] : nameAndParameters.substring(idx + 1, eidx).split(",")
			);
		}
	}

	public boolean isBlank() {
		return value == null || value.equals("");
	}
}
