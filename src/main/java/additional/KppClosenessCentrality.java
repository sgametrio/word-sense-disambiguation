package additional;

import dk.aaue.sna.alg.FloydWarshallAllShortestPaths;
import dk.aaue.sna.alg.centrality.CentralityMeasure;
import dk.aaue.sna.alg.centrality.CentralityResult;

import org.jgrapht.Graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of Dangalchev closeness centrality. Uses floyd-warshall internally to calculate shortest paths.
 *
 * <p>
 * [1] Dangalchev, C. Residual closeness in networks, Physica A 365: 556-564, 2006. (Eq. 2)
 * </p>
 * @param <V> node type
 * @param <E> edge type
 */
public class KppClosenessCentrality<V, E> implements CentralityMeasure<V> {

	private Graph<V, E> graph;
	private FloydWarshallAllShortestPaths<V, E> fw;

	public KppClosenessCentrality(Graph<V, E> graph) {
        this(graph, new FloydWarshallAllShortestPaths<V, E>(graph));
	}

	public KppClosenessCentrality(Graph<V, E> graph, FloydWarshallAllShortestPaths<V, E> fw) {
		this.graph = graph;
        this.fw = fw;
	}

	public CentralityResult<V> calculate() {
		Map<V, Double> cc = new HashMap<V, Double>();
		Set<V> V = graph.vertexSet();
		for (V u : V) {
			double sum = 0.0;
			for (V v : V) {
				// skip reflexiveness
				if (u == v)
					continue;

				// get length of the path
				double length = fw.shortestDistance(u, v);

                // infinite -> there is no path.
				if (Double.isInfinite(length))
                    sum += 0.0;
                else
                    sum += 1.0 / length;
			}
            cc.put(u, sum);
		}
        return new CentralityResult<V>(cc, true);
	}
}