import { Search as SearchIcon } from 'lucide-react';
import '@styles/search.css';

function Search({ value, onChange, placeholder, disabled = false }) {
    return (
        <label className="search">
            <span className="sr-only">{placeholder}</span>
            <SearchIcon className="search__icon" size={16} aria-hidden="true" />
            <input
                type="text"
                className='search-input-table'
                value={value}
                onChange={onChange}
                placeholder={placeholder}
                disabled={disabled}
            />
        </label>
    )
}

export default Search;
