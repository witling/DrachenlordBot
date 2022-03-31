package de.eliaspr.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MarkovChainTransitions {
	private int chainOrder;
	private Map<Integer, List<String>> inner = new HashMap<>();
	private Map<Integer, List<String>> startStates = new HashMap<>();
	private Random ran = new Random();

	public MarkovChainTransitions(int chainOrder) {
		this.chainOrder = chainOrder;
	}

	public void populateFromText(String rawText) {
		String[] tokens = rawText.split(" ");
		List<String> insertingState = new ArrayList<>(this.chainOrder);
		Iterator<String> tokenIterator = Arrays.stream(tokens).iterator();

		for (int i = 0; i < this.chainOrder && tokenIterator.hasNext(); i++) {
			insertingState.add(tokenIterator.next());
		}

		while (tokenIterator.hasNext()) {
			String token = tokenIterator.next();
			if (token.isBlank()) {
				continue;
			}

			this.addTransition(insertingState, token);

			insertingState.remove(0);
			insertingState.add(token);
		}
	}

	public void addTransition(List<String> from, String to) {
		int hash = from.hashCode();
		if (!this.inner.containsKey(hash)) {
			this.inner.put(hash, new ArrayList<>());
			this.startStates.put(hash, new ArrayList<>(from));
		}
		this.inner.get(hash).add(to);
	}

	public List<String> getRandomStartState() {
		int randomIndex = this.getRandomIndex(this.startStates.size());
		List<String> state = (List<String>) this.startStates.values().toArray()[randomIndex];
		return new ArrayList<>(state);
	}

	public String getRandomSuccessor(List<String> state) {
		int hash = state.hashCode();
		if (!this.inner.containsKey(hash)) {
			return null;
		}

		List<String> possibleSuccessors = this.inner.get(hash);
		int randomIndex = this.getRandomIndex(possibleSuccessors.size());
		return possibleSuccessors.get(randomIndex);
	}

	public int getChainOrder() {
		return chainOrder;
	}

	private int getRandomIndex(int max) {
		return this.ran.nextInt(max);
	}
}
