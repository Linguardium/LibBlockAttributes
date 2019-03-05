package alexiil.mc.lib.attributes.fluid.filter;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.RandomAccess;

import com.google.common.collect.Iterators;

import alexiil.mc.lib.attributes.AggregateFilterType;
import alexiil.mc.lib.attributes.fluid.FluidKey;

/** An {@link IFluidFilter} over a predefined array of {@link IFluidFilter}'s. You can either iterate over this object
 * directly or use {@link #getFilterCount()} and {@link #getFilter(int)} to read every filter individually. */
public class AggregateFluidFilter implements IReadableFluidFilter, Iterable<IFluidFilter> {
    public final AggregateFilterType type;
    private final IFluidFilter[] filters;

    public AggregateFluidFilter(AggregateFilterType type, IFluidFilter... filters) {
        if (filters.length < 2) {
            throw new IllegalArgumentException("There's no reason to construct an aggregate stack filter that matches "
                + filters.length + " filters!");
        }
        this.type = type;
        this.filters = filters;
    }

    /** @return An {@link AggregateFluidFilter} that contains both of the given filters. This might not return a new
     *         object if either of the filters contains the other. */
    public static IFluidFilter and(IFluidFilter filterA, IFluidFilter filterB) {
        return combine(AggregateFilterType.ALL, filterA, filterB);
    }

    /** @return An {@link AggregateFluidFilter} that contains both of the given filters. This might not return a new
     *         object if either of the filters contains the other. */
    public static IFluidFilter or(IFluidFilter filterA, IFluidFilter filterB) {
        return combine(AggregateFilterType.ANY, filterA, filterB);
    }

    public static IFluidFilter combine(AggregateFilterType type, IFluidFilter filterA, IFluidFilter filterB) {
        final boolean all = type == AggregateFilterType.ALL;
        if (filterA == filterB) {
            return filterA;
        }
        if (filterA == ConstantFluidFilter.ANYTHING) {
            return all ? filterB : ConstantFluidFilter.ANYTHING;
        }
        if (filterB == ConstantFluidFilter.ANYTHING) {
            return all ? filterA : ConstantFluidFilter.ANYTHING;
        }
        if (filterA == ConstantFluidFilter.NOTHING) {
            return all ? ConstantFluidFilter.NOTHING : filterB;
        }
        if (filterB == ConstantFluidFilter.NOTHING) {
            return all ? ConstantFluidFilter.NOTHING : filterA;
        }
        if (filterA instanceof AggregateFluidFilter && ((AggregateFluidFilter) filterA).type == type) {
            if (filterB instanceof AggregateFluidFilter && ((AggregateFluidFilter) filterB).type == type) {
                IFluidFilter[] filtersA = ((AggregateFluidFilter) filterA).filters;
                IFluidFilter[] filtersB = ((AggregateFluidFilter) filterB).filters;
                IFluidFilter[] filters = new IFluidFilter[filtersA.length + filtersB.length];
                System.arraycopy(filtersA, 0, filters, 0, filtersA.length);
                System.arraycopy(filtersB, 0, filters, filtersA.length, filtersB.length);
                return new AggregateFluidFilter(type, filters);
            }
            IFluidFilter[] from = ((AggregateFluidFilter) filterA).filters;
            IFluidFilter[] filters = new IFluidFilter[from.length + 1];
            System.arraycopy(from, 0, filters, 0, from.length);
            filters[from.length] = filterB;
            return new AggregateFluidFilter(type, filters);
        }
        if (filterB instanceof AggregateFluidFilter && ((AggregateFluidFilter) filterB).type == type) {
            IFluidFilter[] from = ((AggregateFluidFilter) filterB).filters;
            IFluidFilter[] filters = new IFluidFilter[from.length + 1];
            System.arraycopy(from, 0, filters, 0, from.length);
            filters[from.length] = filterA;
            return new AggregateFluidFilter(type, filters);
        }

        if (filterA instanceof ExactFluidFilter) {
            FluidKey exactA = ((ExactFluidFilter) filterA).fluid;
            if (filterB.matches(exactA)) {
                return filterA;
            } else {
                return ConstantFluidFilter.NOTHING;
            }
        }
        if (filterB instanceof ExactFluidFilter) {
            FluidKey exactB = ((ExactFluidFilter) filterB).fluid;
            if (filterA.matches(exactB)) {
                return filterB;
            } else {
                return ConstantFluidFilter.NOTHING;
            }
        }

        return new AggregateFluidFilter(type, filterA, filterB);
    }

    public static IFluidFilter allOf(IFluidFilter... filters) {
        return combine(AggregateFilterType.ALL, filters);
    }

    public static IFluidFilter anyOf(IFluidFilter... filters) {
        return combine(AggregateFilterType.ANY, filters);
    }

    public static IFluidFilter combine(AggregateFilterType type, IFluidFilter... filters) {
        return combine(type, Arrays.asList(filters));
    }

    public static IFluidFilter allOf(List<IFluidFilter> filters) {
        return combine(AggregateFilterType.ALL, filters);
    }

    public static IFluidFilter anyOf(List<IFluidFilter> filters) {
        return combine(AggregateFilterType.ANY, filters);
    }

    public static IFluidFilter combine(AggregateFilterType type, List<IFluidFilter> filters) {
        if (!(filters instanceof RandomAccess)) {
            filters = Arrays.asList(filters.toArray(new IFluidFilter[0]));
        }
        switch (filters.size()) {
            case 0:
                return ConstantFluidFilter.ANYTHING;
            case 1:
                return filters.get(0);
            case 2:
                // I'm assuming this might be faster than putting everything into a list?
                return combine(type, filters.get(0), filters.get(1));
            default: {
                IFluidFilter filter = filters.get(0);
                for (int i = 1; i < filters.size(); i++) {
                    filter = combine(type, filter, filters.get(i));
                }
                return filter;
            }
        }
    }

    @Override
    public boolean matches(FluidKey fluid) {
        if (type == AggregateFilterType.ALL) {
            for (IFluidFilter filter : filters) {
                if (!filter.matches(fluid)) {
                    return false;
                }
            }
            return true;
        } else {
            for (IFluidFilter filter : filters) {
                if (filter.matches(fluid)) {
                    return true;
                }
            }
            return false;
        }
    }

    public int getFilterCount() {
        return filters.length;
    }

    public IFluidFilter getFilter(int index) {
        return filters[index];
    }

    @Override
    public Iterator<IFluidFilter> iterator() {
        return Iterators.forArray(filters);
    }
}