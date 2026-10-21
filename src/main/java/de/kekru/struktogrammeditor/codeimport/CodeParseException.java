package de.kekru.struktogrammeditor.codeimport;

public class CodeParseException extends Exception {

	private static final long serialVersionUID = 1L;
	private final int zeile;

	public CodeParseException(String meldung, int zeile){
		super(meldung);
		this.zeile = zeile;
	}

	public int getZeile(){
		return zeile;
	}

	@Override
	public String getMessage(){
		return zeile > 0 ? "Zeile " + zeile + ": " + super.getMessage() : super.getMessage();
	}
}
