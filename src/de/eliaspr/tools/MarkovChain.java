package de.eliaspr.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class MarkovChain {
	private MarkovChainTransitions transitions = null;

	private MarkovChain(MarkovChainTransitions transitions) {
		this.transitions = transitions;
	}

	public static MarkovChain buildFromFile(Path inputPath, int chainOrder) throws FileNotFoundException, IOException {
		MarkovChainTransitions transitions = new MarkovChainTransitions(chainOrder);
		String rawText = Files.readString(inputPath);

		// normalize input string
		rawText = rawText.toLowerCase();
		rawText = rawText.replaceAll("\n", "").replace("\r", "");
		rawText = rawText.replaceAll("\\.", " . ").replaceAll("!", " ! ").replaceAll(",", " , ").replaceAll(":", " : ");

		transitions.populateFromText(rawText);

		return new MarkovChain(transitions);
	}

	public String generateWords(int n) throws Exception {
		List<String> state = this.transitions.getRandomStartState();
		StringBuilder output = new StringBuilder();

		for (int i = 0; i < n; i++) {
			if (0 < i) {
				output.append(" ");
			}

			// pick successor word for that state
			String successor = this.transitions.getRandomSuccessor(state);
			if (successor == null) {
				state = this.transitions.getRandomStartState();
				successor = this.transitions.getRandomSuccessor(state);
			}

			// if there still is no successor state -> just exit
			if (successor == null) {
				throw new Exception("Fähler. Markov Kette han 1 Problem khat");
			}

			output.append(successor);

			// make sure that at most `ChainOrder` items influence the next successor
			if (this.transitions.getChainOrder() < state.size() + 1) {
				state.remove(0);
			}
			state.add(successor);
		}

		return output.toString();
	}

	public static void main(String[] args) throws Exception {
		Path assetFile = Paths.get(System.getProperty("user.dir")).resolve("assets/aufgaben.txt");
		MarkovChain chain = MarkovChain.buildFromFile(assetFile, 2);
		System.out.println(chain.generateWords(50));
	}
}
